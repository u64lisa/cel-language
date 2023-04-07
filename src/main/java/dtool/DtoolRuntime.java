package dtool;

import dtool.config.DtoolConfig;
import dtool.io.ProjectFolder;
import language.backend.compiler.CompileType;

import java.nio.file.Path;

public abstract class DtoolRuntime {

    protected static final String DEFAULT_CONFIG_NAME = "system.dtool";
    protected static final String DEFAULT_SOURCE_FOLDER = "/src/";
    protected static final String DEFAULT_BUILD_FOLDER = "/build/";
    protected static final String[] STRUCTURE = {
            "src/",
            "resources/",
            "build/"
    };

    public abstract int init();

    public abstract void processLexer();

    public abstract void processParser();

    public abstract void processPreCompiler();

    public abstract void processCompiler(final CompileType compileType);

    public abstract void processFinalize();

    abstract DtoolConfig getConfig();

    public abstract void runTest(String[] args);

    public abstract ProjectFolder getProjectFolder();

    public static DtoolRuntime create(ProjectFolder folder) {
        if (folder == null) {
            throw new IllegalArgumentException("project folder is null");
        }

        return new DefaultDtoolRuntime(folder);
    }


    public abstract Path getSourcePath();
}
