package cli.boot;

import dtool.DtoolRuntime;
import dtool.io.ProjectFolder;
import language.backend.compiler.CompileType;

import java.util.Objects;
import java.util.Scanner;

public class Bootstrap {

    //    DtoolRuntime test = DtoolRuntime
    //                .create(ProjectFolder.of("source_testing"));
    //

    //
    //        test.runTest(args);

    public static void main(String[] args) {
        if (args.length < 2) {
            printHelp();
        }

        final String firstArgument = args[0];
        final String projectName = args[1];

        DtoolRuntime runtime = DtoolRuntime
                .create(ProjectFolder.of(projectName));

        switch (firstArgument) {

            case "--setup" -> {
                System.out.println("initializing workspace...");
                runtime.init();
                System.out.println("finished initializing workspace!");
                System.exit(0);
            }
            case "--compile" -> {
                System.out.println("initializing runtime...");
                runtime.init();
                System.out.println("finished initializing runtime");
                // frontend
                System.out.println("processing lexer...");
                runtime.processLexer();
                System.out.println("processing parser...");
                runtime.processParser();
                // backend
                System.out.println("processing pre-compiler...");
                runtime.processPreCompiler();
                System.out.println("processing compiler...");
                runtime.processCompiler(CompileType.CUSTOM_IR);
                // finish
                System.out.println("finalizing compiler");
                runtime.processFinalize();
                System.out.println("finished compiling project!");
                System.exit(0);
            }
            case "--test" -> {
                System.out.println("initializing tests...");
                runtime.init();
                runtime.runTest(new String[0]);
                System.exit(0);
            }
            case "--check" -> {
                System.out.println("initializing file check...");
                runtime.init();
                // frontend
                runtime.processLexer();
                runtime.processParser();
                System.out.println("finished checking!");
                System.exit(0);
            }
            case "--execute" -> {
                System.out.println("TODO: FINISH THIS LOL");
                System.exit(0);
            }

            default -> printHelp();
        }
    }

    static void printHelp() {
        final Scanner scanner = new Scanner(Objects
                .requireNonNull(Bootstrap.class.getResourceAsStream("/help.txt")));

        final String helpTitle = scanner.useDelimiter("\\A").next();
        System.err.println(helpTitle);
        System.exit(-1);
    }

}
