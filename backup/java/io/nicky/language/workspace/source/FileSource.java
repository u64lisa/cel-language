package io.nicky.language.workspace.source;

import language.utils.Pair;
import language.utils.sneak.SneakyThrow;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSource implements Source {

    private final Path path;
    private String directory, file, source;

    public FileSource(Path path) {
        this.path = path;
    }

    @Override
    public Source extract() {
        Pair<String, String> sourceDirectory = getSourceDirectory(path.toFile().toString());

        file = sourceDirectory.getFirst();
        directory = sourceDirectory.getLast();

        return this;
    }

    @Override
    public Source readSource() {
        SneakyThrow.sneaky(() -> source = readString(this.path)).get();
        return this;
    }

    public Pair<String, String> getSourceDirectory(String dir) {
        final String[] parts = dir.split("\\\\");
        final String source = parts[parts.length - 1];
        final StringBuilder sourceDirectory = new StringBuilder();
        for (int index = 0; index < parts.length - 1; index++) {
            final String current = parts[index];
            sourceDirectory.append(current).append("\\");
        }
        return new Pair<>(source, sourceDirectory.toString().replace("\\.", ""));
    }


    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public String getFile() {
        return file;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public Path getFilePath() {
        return path;
    }

    public String readString(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "FileSource{" +
                "path=" + path +
                ", directory='" + directory + '\'' +
                ", file='" + file + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
