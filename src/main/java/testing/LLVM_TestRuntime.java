package testing;

import dtool.DtoolRuntime;
import dtool.io.ProjectFolder;
import language.backend.compiler.CompileType;
import language.frontend.parser.nodes.TreePrinter;

public class LLVM_TestRuntime {

    static {
        System.setProperty("lang.debug", "true");
    }

    public static void main(String[] args) {
        args = new String[]{"test"};

        DtoolRuntime test = DtoolRuntime
                .create(ProjectFolder.of("test_space_llvm"));

        test.init();
        // frontend
        test.processLexer();
        test.processParser();
        // backend
        test.processPreCompiler();

        test.ast(body -> {
            String ast = TreePrinter.print(body);
            System.out.println(ast);
        });

        test.processCompiler(CompileType.LLVM); // playground thingy
        // finish
        test.processFinalize();


    }

}
