package dtool;

import dtool.config.DtoolConfig;
import dtool.io.ProjectFolder;

public abstract class DtoolRuntime {

    protected static final String DEFAULT_CONFIG_NAME = "system.dtool";
    protected static final String DEFAULT_SOURCE_FOLDER = "/src/";
    protected static final String DEFAULT_BUILD_FOLDER = "/build/";
    protected static final String[] STRUCTURE = {
            "src/",
            "resources/",
            "build/"
    };

    public abstract void init();

    public abstract void processLexer();

    public abstract void processParser();

    public abstract void processPreCompiler();

    public abstract void processCompiler();

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


}
