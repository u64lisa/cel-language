package testing;

import io.nicky.language.workspace.ProjectWorkspace;

public class Testing {

    static {
        System.setProperty("lang.debug", "true");
    }

    public static void main(String[] args) {
        args = new String[]{"test"};

        new ProjectWorkspace("testing")
                .init()
                .checkFolder()
                .compile()
                .execute(args)
                .displayProfiling()
                .ast(nodes -> {
                    // testing only

                    //final ByteCodeCompiler compiler = new X86Compiler();
                    //compiler.initialize();
                    //
                    //System.out.println("-".repeat(120));
                    //System.out.println(new String(compiler.compileBytecode(nodes)));

                })
                .tokens(tokens -> {

                })
        ;
    }

}
