package language.frontend.parser.units;

import dtool.DefaultDtoolRuntime;
import dtool.DtoolRuntime;
import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import language.frontend.lexer.Lexer;
import language.frontend.lexer.token.Token;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.nodes.expressions.ImportNode;
import language.frontend.parser.results.ParseResult;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class Linker {

    public static final Logger SYSTEM_LOGGER = ImplLogger.getInstance();
    
    private final List<Node> abstractSyntaxTree;

    private final HashMap<String, List<Node>> projectImports = new HashMap<>();

    private final DtoolRuntime runtime;

    public Linker(final DtoolRuntime runtime, List<Node> abstractSyntaxTree) throws Exception {
        this.runtime = runtime;
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

    void extractImports(final List<Node> nodes) throws Exception {
        for (Node node : nodes) {
            if (node instanceof final ImportNode importNode && node.getNodeType() == NodeType.IMPORT) {

                if (projectImports.containsKey(importNode.getFileName().asString()))
                    continue;

                final List<Node> importAst = this.resolveImportAst(importNode);

                projectImports.put(importNode.getFileName().asString(), importAst);

                if (importAst.stream().anyMatch(stream -> stream.getNodeType() == NodeType.IMPORT))
                    this.extractImports(importAst);
            }
        }

        nodes.removeIf(node -> node.getNodeType() == NodeType.IMPORT);
    }


    List<Node> resolveImportAst(ImportNode node) throws Exception {
        final String importedFile = node.getFileName().asString()
                .replace("|", "/");

        final String projectPath = runtime.getSourcePath()
                + File.separator + importedFile + DefaultDtoolRuntime.SRC_FILE_SUFFIX;

        final Path path = Path.of(projectPath);
        final File sourceFile = path.toFile();

        if (sourceFile.exists() && sourceFile.isFile()) {
            Scanner scanner = new Scanner(sourceFile);
            final String fileContent = scanner.useDelimiter("\\A").next();
            scanner.close();

            Lexer lexer = new Lexer(projectPath, fileContent);
            List<Token> tokens = lexer.lex();

            Parser parser = Parser.getParser(tokens);

            ParseResult<Node> ast = parser.parse();
            if (ast.getLanguageError() != null) {
                SYSTEM_LOGGER.fail(importedFile,
                        fileContent, ast.getLanguageError());
                return null;
            }

            BodyNode body = (BodyNode) ast.getNode().optimize();
            return body.statements;
        }

        throw new RuntimeException("tried to import file that does not exist '" + projectPath + "'!");
    }


}
