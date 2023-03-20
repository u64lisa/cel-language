package io.nicky.language.workspace.source;

import io.nicky.language.Language;
import language.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SourceService {

    private static SourceService instance;

    private final Map<Path, Source> globalSources = new ConcurrentHashMap<>();

    public enum State {
        EXECUTE,
        COMPILE
    }

    public SourceService() {
        instance = this;
    }


    public Source loadAsSource(final State state, final Path path) {
        final List<Path> sources = new ArrayList<>();
        final StringBuilder sourceRaw = new StringBuilder();

        try {
            if (path.toFile().toString().endsWith(Language.SRC_FILE_SUFFIX) ||
                    path.toFile().toString().endsWith(Language.IR_FILE_SUFFIX)) {
                final Source source = new FileSource(path);
                source.extract();

                if (state == State.COMPILE)
                    source.readSource();

                return source;
            }
            for (Path current : Files.walk(path)
                    .filter(current -> current.toFile().getName().endsWith(Language.SRC_FILE_SUFFIX) ||
                            current.toFile().getName().endsWith(Language.IR_FILE_SUFFIX))
                    .filter(current -> current.toFile().isFile()).toList()) {
                sources.add(current);

                final Scanner scanner = new Scanner(current.toFile());
                while (scanner.hasNextLine())
                    sourceRaw.append(scanner.nextLine()).append('\n');

                scanner.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (sources.size() <= 0)
            throw new RuntimeException("No Sources found!");

        Source source = new AdditionalFileSource(sources.get(0), sourceRaw.toString()).extract();
        if (state == State.COMPILE)
            source.readSource();

        return source;
    }

    public List<Source> loadAsSources(final State state, final Path... paths) {
        final List<File> sourceFiles = new CopyOnWriteArrayList<>();
        final List<Source> sources = new CopyOnWriteArrayList<>();

        for (final Path path : paths) {
            if (path.toFile().isDirectory()) {
                this.listFilesInDirectory(path.toAbsolutePath().toString(), sourceFiles);
                continue;
            }
            sourceFiles.add(path.toFile());
        }

        for (File sourceFile : sourceFiles) {
            final Source source = new FileSource(sourceFile.toPath()).extract();

            if (state == State.COMPILE)
                source.readSource();

            globalSources.put(sourceFile.toPath(), source);
            sources.add(source);
        }
        return sources;
    }

    private void listFilesInDirectory(final String directoryName, final List<File> files) {
        File directory = new File(directoryName);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listFilesInDirectory(file.getAbsolutePath(), files);
                }
            }
    }

    public Pair<String, String> getSourceDirectory(String dir) {
        int ind = Math.max(dir.lastIndexOf('\\'), dir.lastIndexOf('/'));
        if (ind == -1)
            return new Pair<>(dir, ".");

        return new Pair<>(
                dir.substring(ind),
                dir.substring(0, ind)
        );
    }

    public Map<Path, Source> getGlobalSources() {
        return globalSources;
    }

    public static SourceService getInstance() {
        return instance;
    }
}
