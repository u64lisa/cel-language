package dtool;

import dtool.config.DtoolConfig;
import dtool.io.ProjectFolder;
import dtool.io.tree.FileTree;
import dtool.io.walker.FileWalker;
import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import dtool.logger.errors.LanguageException;
import dtool.logger.profiler.LanguageProfiler;
import dtool.source.SourceFile;
import dtool.source.SourceSuffix;
import dtool.source.scan.DirectoryScanner;
import dtool.utils.DynamicOptional;
import language.backend.compiler.bytecode.ChunkBuilder;
import language.backend.compiler.bytecode.Compiler;
import language.backend.compiler.bytecode.FunctionType;
import language.backend.compiler.bytecode.ir.Compressor;
import language.backend.compiler.bytecode.types.objects.ClassObjectType;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.frontend.lexer.Lexer;
import language.frontend.lexer.token.Token;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.results.ParseResult;
import language.frontend.parser.units.Linker;
import language.vm.VirtualMachine;
import language.vm.VirtualMachineResult;
import language.vm.library.LibraryClassLoader;
import language.vm.library.NativeContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DefaultDtoolRuntime extends DtoolRuntime {

    public static final LanguageProfiler PROFILER = new LanguageProfiler();
    public static final Logger LOGGER = ImplLogger.getInstance();

    public static final Compressor COMPRESSOR = new Compressor();

    public static final String IR_FILE_SUFFIX = ".dem";
    public static final String SRC_FILE_SUFFIX = ".ag";

    private final List<SourceFile> sources = new ArrayList<>();

    private final FileWalker walker = FileWalker.create();
    private final ProjectFolder projectFolder;
    private final DtoolConfig config;

    public DefaultDtoolRuntime(final ProjectFolder projectFolder) {
        this.projectFolder = projectFolder;
        this.config = new DtoolConfig(projectFolder.projectRoot(), DEFAULT_CONFIG_NAME);
    }

    @Override
    public void init() {
        walker.walk(this.projectFolder, STRUCTURE);
        config.load();
    }

    @Override
    public void processLexer() {
        Path sourcePath = null;

        if (config.getConfigTree()
                .getProjectProperties()
                .hasAttribute("source.path")) {

            final String path = config.getConfigTree()
                    .getProjectProperties()
                    .get("source.path");

            sourcePath = Path.of(this.projectFolder.projectRoot().toString(),
                    path);
        } else {
            sourcePath = Path.of(this.projectFolder.projectRoot().toString(),
                    DEFAULT_SOURCE_FOLDER);
        }

        final DirectoryScanner scanner = new DirectoryScanner(sourcePath.toString());
        final FileTree fileTree = scanner.getFileTree();

        for (SourceFile source : fileTree.sources()) {
            if (source.getSuffix() == SourceSuffix.SOURCE) {
                Lexer lexer = new Lexer(source.getPath(), source.getSource());

                List<Token> tokenList = lexer.lex();

                source.setTokenList(tokenList);
            }

            sources.add(source);
        }

    }

    @Override
    public void processParser() {
        for (SourceFile source : sources) {
            Parser parser = Parser.getParser(source.getTokenList());

            ParseResult<Node> result = parser.parse();
            if (result.getLanguageError() != null)
                LOGGER.fail(source.getPath(), source.getSource(), result.getLanguageError());

            Node ast = result.getNode();
            source.getAstNode().setValue(ast);
        }
    }

    @Override
    public void processPreCompiler() {
        for (SourceFile source : sources) {
            if (!source.getAstNode().isPresent())
                throw new RuntimeException(source.getPath() + " has no AST to parse!");

            // todo process macros and such

            BodyNode body = (BodyNode) source.getAstNode().getValue().optimize();

            final Linker classImportService = new Linker(body.statements);
            source.getAst().setValue(classImportService.getMergedTree());
        }
    }

    @Override
    public void processCompiler() {
        for (SourceFile source : sources) {
            System.out.printf("--------------------- %s -----------------%n", source.getFileName());

            // ()<> -> void
            Compiler compiler = new Compiler(FunctionType.SOURCE, source.getSource(), ClassObjectType.EMPTY);
            compiler.chunk().globals = new HashMap<>();
            compiler.chunk().globals.putAll(LibraryClassLoader.LIBRARY_TYPES);
            compiler.chunk().globals.putAll(NativeContext.GLOBAL_TYPES);

            ByteCode byteCode = compiler.compileBlock(source.getAst().getValue());

            final byte[] dumped = COMPRESSOR.compress(byteCode.dumpBytes());

            source.getByteCode().setValue(byteCode);
            source.getDumpedByteCode().setValue(dumped);
        }

        String mainClass = config.getConfigTree().getProjectProperties()
                .getOrDefault("main", "?");

        DynamicOptional<SourceFile> mainFile = new DynamicOptional<>();

        for (SourceFile source : sources) {
            if (mainFile.isPresent())
                break;

            if (source.getFileName().equalsIgnoreCase(mainClass)) {
                mainFile.setValue(source);
            }
        }

        if (!mainFile.isPresent())
            throw new RuntimeException("Unable to find main class by name: " + mainClass);

        final SourceFile mainEntry = mainFile.getValue();

        try {
            final Path buildPath = Path.of(this.projectFolder.projectRoot().toString(),
                    DEFAULT_BUILD_FOLDER);

            final File outFile = new File(buildPath.toFile(), "out" + IR_FILE_SUFFIX);

            if (outFile.exists() && !outFile.delete())
                throw new IllegalStateException("can't delete file");

            if (!outFile.createNewFile())
                throw new IllegalStateException("can't create new file");

            Files.write(outFile.toPath(), mainEntry.getDumpedByteCode().getValue(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.fail(mainEntry.getPath(), mainEntry.getSource(), new LanguageException(
                    LanguageException.Type.COMPILER,
                    "Internal",
                    "Could not write to file"
            ));
        }
    }

    @Override
    public void processFinalize() {

    }

    @Override
    public void runTest(final String[] args) {
        final Path buildPath = Path.of(this.projectFolder.projectRoot().toString(),
                DEFAULT_BUILD_FOLDER);

        final File outFile = new File(buildPath.toFile(), "out" + IR_FILE_SUFFIX);

        byte[] arr = new byte[0];
        try {
            arr = COMPRESSOR.decompress(Files.readAllBytes(outFile.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] finalArr = arr;
        ByteCode func = ChunkBuilder.build(finalArr);


        final VirtualMachine virtualMachine = new VirtualMachine(func).trace(outFile.toString());

        VirtualMachineResult res = virtualMachine.run();
        if (res == VirtualMachineResult.ERROR) return;
        virtualMachine.finish(args);

    }

    @Override
    public DtoolConfig getConfig() {
        return config;
    }

    @Override
    public ProjectFolder getProjectFolder() {
        return projectFolder;
    }

}