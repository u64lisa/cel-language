package io.nicky.language.workspace.config;

import io.nicky.language.Language;
import language.utils.sneak.SneakyThrow;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectConfiguration implements Configuration {

    private final Map<String, String> configValues = new ConcurrentHashMap<>();

    public String artifactId, groupId, projectVersion, languageVersion, mainClass, compiledSource;
    public boolean monitor;

    private final Path projectDirectory;

    public ProjectConfiguration(String projectDirectory) {
        this.projectDirectory = Path.of(projectDirectory);
    }

   public ProjectConfiguration(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    @Override
    public void index() {
        final File projectConfig = Paths.get(projectDirectory.toString(), File.separator + "project.angel").toFile();
        if (projectConfig.exists() && projectConfig.isFile()) {

            SneakyThrow.sneaky(() -> {

                final Scanner scanner = new Scanner(projectConfig);

                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine().trim().replace(" ", "");

                    if (line.startsWith("#") || !line.contains("="))
                        continue;

                    final String[] parts = line.split("=");

                    if (parts.length < 2)
                        throw new RuntimeException("Not enough parameters in scope for line: \"" + line + "\"");

                    configValues.put(parts[0], parts[1]);
                }

            }).run();

            return;
        }

        SneakyThrow.sneaky(() -> {

            final FileWriter fileWriter = new FileWriter(projectConfig);
            fileWriter.write("artifactId=?\n");
            fileWriter.write("groupId=?\n");
            fileWriter.write("projectVersion=?\n");
            fileWriter.write("language-version=" + Language.VERSION + "\n");
            fileWriter.write("mainClass=?\n");
            fileWriter.write("compiledSource=/build/binaries%s\n".formatted(Language.IR_FILE_SUFFIX));

            fileWriter.flush();
            fileWriter.close();

            this.index();

        }).run();

    }

    @Override
    public void parse() {
        SneakyThrow.sneaky(() -> {
            this.artifactId = this.configValues
                    .getOrDefault("artifactId", "?");
            this.groupId = this.configValues
                    .getOrDefault("groupId", "?");
            this.projectVersion = this.configValues
                    .getOrDefault("projectVersion", "?");
            this.languageVersion = this.configValues
                    .getOrDefault("language-version", Language.VERSION);
            this.mainClass = this.configValues
                    .getOrDefault("mainClass", "?");
            this.compiledSource = this.configValues
                    .getOrDefault("compiledSource",
                            "/build/binaries%s\n".formatted(Language.IR_FILE_SUFFIX));

            this.monitor = this.configValues.getOrDefault("process-monitoring", "false")
                    .equals("true");

            for (Field declaredField : this.getClass().getDeclaredFields()) {
                final Object value = declaredField.get(this);
                if (value instanceof String) {
                    if (((String) value).equalsIgnoreCase("?"))
                        throw new RuntimeException("Missing parameter for project: \"" + declaredField.getName() + "\"");
                }
            }
        }).run();

    }

}
