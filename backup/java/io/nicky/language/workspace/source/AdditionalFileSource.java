package io.nicky.language.workspace.source;

import language.utils.Pair;

import java.nio.file.Path;

public class AdditionalFileSource implements Source {

    private String directory, file;
    private final Path path;
    private final String source;

    public AdditionalFileSource(Path path, String extractedSource) {
        this.path = path;
        this.source = extractedSource;
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
        return this;
    }

    public Pair<String, String> getSourceDirectory(String dir) {
        final String[] parts = dir.split("\\\\");
        final String source = parts[parts.length -1];
        final StringBuilder sourceDirectory = new StringBuilder();
        for (int index = 0; index < parts.length - 1; index++) {
            final String current = parts[index];
            sourceDirectory.append(current).append("\\");
        }
        return new Pair<>(source, sourceDirectory.toString());
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


    @Override
    public String toString() {
        return "AdditionalFileSource{" +
                "directory='" + directory + '\'' +
                ", file='" + file + '\'' +
                ", path=" + path +
                ", source='" + source + '\'' +
                '}';
    }
}
