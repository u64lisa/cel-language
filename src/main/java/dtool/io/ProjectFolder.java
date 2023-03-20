package dtool.io;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public record ProjectFolder(String projectName, String projectRoot) {

    public Path getAsPath() {
        return Path.of(projectRoot);
    }

    public static ProjectFolder of(final String file) {
        final File source = Paths.get(file).toFile();

        if (!source.exists()) {
            String name = source.getName();

            return new ProjectFolder(name, source.getAbsolutePath());
        }
        if (source.isDirectory()) {
            String name = source.getName();

            return new ProjectFolder(name, source.getAbsolutePath());
        } else {
            return null;
        }
    }
}
