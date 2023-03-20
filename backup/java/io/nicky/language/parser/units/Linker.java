package language.frontend.parser.units;

import io.nicky.language.Language;
import language.backend.compiler.Compiler;
import language.frontend.lexer.Lexer;
import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import dtool.logger.errors.LanguageException;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.nodes.expressions.ImportNode;
import language.frontend.parser.results.ParseResult;
import language.utils.Pair;
import language.utils.sneak.SneakyThrow;
import language.frontend.lexer.token.Token;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Linker {
    
    public static final Logger SYSTEM_LOGGER = ImplLogger.getInstance();
    
    private final List<Node> abstractSyntaxTree;

    private final HashMap<String, List<Node>> projectImports = new HashMap<>();

    public Linker(List<Node> abstractSyntaxTree) {
        this.abstractSyntaxTree = abstractSyntaxTree;
        this.extractImports(abstractSyntaxTree);
    }

    public List<Node> getMergedTree() {
        final ArrayList<Node> nodes = new ArrayList<>();
        for (Node node : abstractSyntaxTree) {
            if (node.getNodeType() == NodeType.USE) {
                nodes.add(node);
                abstractSyntaxTree.remove(node);
            }
        }
        for (Map.Entry<String, List<Node>> entry : projectImports.entrySet()) {
            entry.getValue().removeIf(node -> node.getNodeType() == NodeType.IMPORT);
            nodes.addAll(entry.getValue());
        }
        nodes.addAll(abstractSyntaxTree);
        return nodes;
    }

    void extractImports(final List<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof final ImportNode importNode && node.getNodeType() == NodeType.IMPORT) {

                if (projectImports.containsKey(importNode.fileName.asString()))
                    continue;

                final List<Node> importAst = this.resolveImportAst(importNode);

                projectImports.put(importNode.fileName.asString(), importAst);

                if (importAst.stream().anyMatch(stream -> stream.getNodeType() == NodeType.IMPORT))
                    this.extractImports(importAst);
            }
        }

        nodes.removeIf(node -> node.getNodeType() == NodeType.IMPORT);
    }


    List<Node> resolveImportAst(ImportNode node) {

        try {
            String fileName = Paths.get(System.getProperty("execution_compile_directory"))
                    .toFile()
                    .getAbsolutePath()
                    .replace("\\.\\", "\\")
                    .replace("\\..\\", "\\")
                    + File.separator +
                    node.fileName
                            .asString()
                            .replace('.', File.separatorChar) + Language.SRC_FILE_SUFFIX;

            if (node.fileName.asString().toLowerCase(Locale.ROOT).contains("std")) {
                return SneakyThrow.sneak(() -> {

                    final String internalPath = "./std/" + (node.fileName
                            .asString()
                            .toLowerCase(Locale.ROOT))
                            .replaceAll("::", "/")
                            .replace("std/", "")

                            + Language.SRC_FILE_SUFFIX;

                    final InputStream stdLibrary = Compiler.class.getClassLoader().getResourceAsStream(internalPath);
                    if (stdLibrary == null)
                        throw new IOException("Internal file not found: " + internalPath);

                    final StringBuilder library = new StringBuilder();
                    final Scanner scanner = new Scanner(stdLibrary);

                    while (scanner.hasNextLine())
                        library.append(scanner.nextLine()).append("\n");

                    Pair<List<Node>, LanguageException> ast = getAst(fileName, library.toString());

                    if (ast.getLast() != null) {
                        SYSTEM_LOGGER.fail(ast.getLast());
                        return null;
                    }

                    return (ast.getFirst());
                });
            }
            Path path = Path.of(fileName);
            if (Files.exists(path)) {

                Pair<List<Node>, LanguageException> ast = getAst(fileName, readString(path));

                if (ast.getLast() != null) {
                    SYSTEM_LOGGER.fail(ast.getLast());
                    return null;
                }

                return (ast.getFirst());
            }

            throw new IOException("File not found");
        } catch (final Throwable throwable) {
            SYSTEM_LOGGER.fail(new LanguageException(LanguageException.Type.COMPILER, node.getStartPosition(), node.getEndPosition(),
                    "Import Error", "Couldn't import file (" + throwable.getMessage() + ")"));
            return null;
        }
    }

    public Pair<List<Node>, LanguageException> getAst(String file, String context) {
        Lexer lexer = new Lexer(file, context);
        List<Token> tokens = lexer.lex();

        Parser parser = Parser.getParser(tokens);

        ParseResult<Node> ast = parser.parse();
        if (ast.getLanguageError() != null)
            return new Pair<>(null, ast.getLanguageError());

        BodyNode body = (BodyNode) ast.getNode().optimize();
        return new Pair<>(body.statements, null);
    }

    public String readString(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

}
