package testing;

import dtool.DtoolRuntime;
import dtool.io.ProjectFolder;
import language.backend.compiler.CompileType;

public class IR_TestRuntime {

    static {
        System.setProperty("lang.debug", "true");
    }

    public static void main(String[] args) {
        args = new String[]{"test"};

        DtoolRuntime test = DtoolRuntime
                .create(ProjectFolder.of("test_space_ir"));

        test.init();
        // frontend
        test.processLexer();
        test.processParser();
        // backend
        test.processPreCompiler();
        test.processCompiler(CompileType.CUSTOM_IR);
        // finish
        test.processFinalize();

        test.runTest(args);
    }


}
