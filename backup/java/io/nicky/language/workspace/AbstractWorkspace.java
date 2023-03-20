package io.nicky.language.workspace;

import io.nicky.language.Language;
import language.frontend.lexer.Lexer;
import language.frontend.lexer.token.Token;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.results.ParseResult;
import io.nicky.language.workspace.config.ProjectConfiguration;
import io.nicky.language.workspace.exception.WorkplaceInitializationException;
import io.nicky.language.workspace.source.Source;
import io.nicky.language.workspace.source.SourceService;
import io.nicky.language.workspace.tasks.CompilerExecutable;
import io.nicky.language.workspace.tasks.RunExecutable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public abstract class AbstractWorkspace implements Workspace {

    private final ProjectConfiguration configuration;

    private final Path sourceDirectory;
    private final Path binaryDirectory;

    public AbstractWorkspace(ProjectConfiguration configuration, Path sourceDirectory, Path binaryDirectory) {
        this.configuration = configuration;
        this.sourceDirectory = sourceDirectory;
        this.binaryDirectory = binaryDirectory;

    }

    @Override
    public Workspace init() {
        System.setProperty("execution_compile_directory", sourceDirectory.toFile().getParent());
        return this;
    }

    @Override
    public Workspace checkFolder() {
        Language.PROFILER.profileSegment("folder structure", () -> {
            {
                if (sourceDirectory.toFile().getName().endsWith(Language.SRC_FILE_SUFFIX) && !sourceDirectory.toFile().exists())
                    throw new WorkplaceInitializationException("Expected a source file but couldn't localize file!");
                if (!sourceDirectory.toFile().exists() && !sourceDirectory.toFile().mkdirs())
                    throw new WorkplaceInitializationException("Can't create language running folder");
            }

            {
                final File binaryFile = binaryDirectory.toFile();
                if (!binaryFile.getName().toLowerCase(Locale.ROOT).endsWith(Language.IR_FILE_SUFFIX)) {
                    if (!binaryDirectory.toFile().exists() && !binaryDirectory.toFile().mkdirs())
                        throw new WorkplaceInitializationException("Can't create language compiler folder");
                }
            }
        });
        return this;
    }

    @Override
    public Workspace compile() {
        final CompilerExecutable compilerExecutable = new CompilerExecutable(sourceDirectory, binaryDirectory);
        compilerExecutable.compile();
        return this;
    }

    @Override
    public Workspace execute(final String[] arguments) {
        RunExecutable runExecutable = new RunExecutable(binaryDirectory);
        runExecutable.execute(arguments);
        return this;
    }

    @Override
    public Workspace displayProfiling() {
        if (configuration.monitor)
            Language.PROFILER.printProfileResults();

        return this;
    }

    @Override
    public Workspace tokens(final Consumer<List<Token>> tokensConsumer) {
        final Source source = new SourceService().loadAsSource(SourceService.State.COMPILE, sourceDirectory);
        final String file = (source.getDirectory().endsWith(File.separator)
                ? source.getDirectory() : source.getDirectory() + File.separator) + source.getFile();

        final String context = source.getSource();

        Lexer lexer = new Lexer(file, context);

        tokensConsumer.accept(lexer.lex());

        return this;
    }

    @Override
    public Workspace ast(final Consumer<List<Node>> abstractSyntaxTree) {
        final Source source = new SourceService().loadAsSource(SourceService.State.COMPILE, sourceDirectory);
        final String file = (source.getDirectory().endsWith(File.separator) ?
                source.getDirectory() : source.getDirectory() + File.separator) + source.getFile();
        final String context = source.getSource();

        Lexer lexer = new Lexer(file, context);

        List<Token> tokens = lexer.lex();

        Parser parser = Parser.getParser(tokens);

        ParseResult<Node> ast = parser.parse();

        if (ast.getLanguageError() != null)
            Language.LOGGER.fail(ast.getLanguageError());

        BodyNode body = (BodyNode) ast.getNode().optimize();

        abstractSyntaxTree.accept(body.statements);

        return this;
    }

}
