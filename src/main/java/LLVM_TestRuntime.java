import dtool.DtoolRuntime;
import dtool.io.ProjectFolder;
import language.backend.compiler.CompileType;

public class LLVM_TestRuntime {

    static {
        System.setProperty("lang.debug", "true");
    }

    public static void main(String[] args) {
        args = new String[]{"test"};

        DtoolRuntime test = DtoolRuntime
                .create(ProjectFolder.of("std"));

        test.init();
        // frontend
        test.processLexer();
        test.processParser();
        // backend
        test.processPreCompiler();
        //test.processCompiler(CompileType.CUSTOM_IR);
        //test.processCompiler(CompileType.ASM_64x86);
        test.processCompiler(CompileType.LLVM); // playground thingy
        // finish
        test.processFinalize();

        //test.runTest(args);
    }

}