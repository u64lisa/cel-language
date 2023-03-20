package io.nicky.language.workspace;

import io.nicky.language.workspace.config.ProjectConfiguration;

import java.io.*;
import java.nio.file.Path;

public class ProjectWorkspace extends AbstractWorkspace {

    public ProjectWorkspace(final String workspaceName) {
        super(new ProjectConfiguration(workspaceName), resolveMainClass(workspaceName), resolveBinaryOut(workspaceName));
    }

    public static Path resolveMainClass(final String project) {
        ProjectConfiguration configuration = new ProjectConfiguration(project);
        configuration.index();
        configuration.parse();

        final Path path = Path.of("./" + project + configuration.mainClass);
        final File sourceFile = path.toFile();

        if (sourceFile.exists() && sourceFile.isFile())
            return Path.of(sourceFile.getAbsolutePath());

        if (checkWorkspace(project)) {
            throw new NullPointerException("Could not find main class, file does not exist");
        }

        System.out.println("Created workspace exiting...");
        System.exit(0);
        return null;
    }

    public static Path resolveBinaryOut(final String project) {
        ProjectConfiguration configuration = new ProjectConfiguration("testing");
        configuration.index();
        configuration.parse();

        final Path path = Path.of("./" + project + configuration.compiledSource);
        final File sourceFile = path.toFile();

        return Path.of(sourceFile.getAbsolutePath());
    }

    public static boolean checkWorkspace(final String projectName) {
        final File workspaceRoot = new File(projectName);

        if (workspaceRoot.exists())
            return true;

        if (workspaceRoot.mkdirs()) {
            checkWorkspaceFolder(workspaceRoot, "build");
            checkWorkspaceFolder(workspaceRoot, "src");

            checkWorkspaceFolder(workspaceRoot, "src/main/cel");

            createExampleMain(workspaceRoot);

            ProjectConfiguration configuration = new ProjectConfiguration(workspaceRoot.toPath());
            configuration.index();
        }

        return false;
    }

    public static void createExampleMain(final File root) {

        try {
            final InputStream inputStream = ProjectWorkspace.class.getResourceAsStream("/example_main.ag");

            final FileOutputStream fileOutputStream = new FileOutputStream(new File(
                    root, "src/main/cel/example.ag"
            ));

            assert inputStream != null: "example main not found!";

            fileOutputStream.write(inputStream.readAllBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkWorkspaceFolder(final File root, final String file) {
        final File toCreate = new File(root, file);

        if (!toCreate.exists() && !toCreate.mkdirs())
            throw new RuntimeException("error while creating folder: " + toCreate);
    }

}
