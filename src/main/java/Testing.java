import dtool.DtoolRuntime;
import dtool.io.ProjectFolder;

public class Testing {

    static {
        System.setProperty("lang.debug", "true");
    }

    public static void main(String[] args) {
        args = new String[]{"test"};

        DtoolRuntime test = DtoolRuntime
                .create(ProjectFolder.of("./source_testing"));

        test.init();
        // frontend
        test.processLexer();
        test.processParser();
        // backend
        test.processPreCompiler();
        test.processCompiler();
        // finish
        test.processFinalize();

        test.runTest(args);
    }

}
