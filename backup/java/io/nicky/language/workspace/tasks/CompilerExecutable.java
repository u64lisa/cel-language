package io.nicky.language.workspace.tasks;

import io.nicky.language.Language;
import language.backend.compiler.Compiler;
import language.backend.compiler.FunctionType;
import language.backend.compiler.bytecode.types.objects.ClassObjectType;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.frontend.lexer.Lexer;
import language.frontend.lexer.token.Token;
import language.vm.library.LibraryClassLoader;
import language.vm.library.NativeContext;
import dtool.logger.errors.LanguageException;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.results.ParseResult;
import language.frontend.parser.units.Linker;
import io.nicky.language.workspace.source.Source;
import io.nicky.language.workspace.source.SourceService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public final class CompilerExecutable implements CompileTask {

    private final Path compileDirectory;
    private final Path binaryDirectory;
    private final SourceService sourceService;

    private List<Node> abstractSyntaxTree;

    public CompilerExecutable(Path compileDirectory, Path binaryDirectory) {
        this.compileDirectory = compileDirectory;
        this.binaryDirectory = binaryDirectory;

        this.sourceService = new SourceService();
    }

    @Override
    public void compile() {
        final Source source = sourceService
                .loadAsSource(SourceService.State.COMPILE, compileDirectory);

        final File binaryFile = binaryDirectory.toFile();

        if (!binaryFile.exists() && !binaryFile.mkdir())
            throw new RuntimeException("Can't create compile folder!");

        final String file = (source.getDirectory().endsWith(File.separator) ? source.getDirectory() :
                source.getDirectory() + File.separator) + source.getFile();

        final String context = source.getSource();
        final Path path = binaryDirectory;

        final List<Token> tokens = Language.PROFILER.profileSegment("lexer", () -> {
            final Lexer lexer = new Lexer(file, context);
            return lexer.lex();
        });

        final ParseResult<Node> syntaxTree = Language.PROFILER.profileSegment("parser", () -> {
            Parser parser = Parser.getParser(tokens);

            ParseResult<Node> ast = parser.parse();
            if (ast.getLanguageError() != null)
                Language.LOGGER.fail(ast.getLanguageError());

            return ast;
        });

        abstractSyntaxTree = Language.PROFILER.profileSegment("optimizing", () -> {
            BodyNode body = (BodyNode) syntaxTree.getNode().optimize();

            return body.statements;
        });

        Language.PROFILER.profileSegment("linker", () -> {
            final Linker classImportService = new Linker(abstractSyntaxTree);
            abstractSyntaxTree = classImportService.getMergedTree();
        });

        final ByteCode byteCode = Language.PROFILER.profileSegment("compiler", () -> {
            // ()<> -> void
            Compiler compiler = new Compiler(FunctionType.SOURCE, context, ClassObjectType.EMPTY);
            compiler.chunk().globals = new HashMap<>();
            compiler.chunk().globals.putAll(LibraryClassLoader.LIBRARY_TYPES);
            compiler.chunk().globals.putAll(NativeContext.GLOBAL_TYPES);

            return compiler.compileBlock(abstractSyntaxTree);
        });

        final byte[] dumped = Language.PROFILER.profileSegment("compression",
                () -> Language.COMPRESSOR.compress(byteCode.dumpBytes()));

        Language.PROFILER.profileSegment("writing", () -> {
            try {
                final File outFile = path.toFile();

                if (outFile.exists() && !outFile.delete())
                    throw new IllegalStateException("can't delete file");

                if (!outFile.createNewFile())
                    throw new IllegalStateException("can't create new file");

                Files.write(path, dumped, StandardOpenOption.WRITE);
            } catch (IOException e) {
                e.printStackTrace();
                Language.LOGGER.fail(new LanguageException(
                        LanguageException.Type.COMPILER,
                        "Internal",
                        "Could not write to file"
                ));
            }
        });

    }

}
