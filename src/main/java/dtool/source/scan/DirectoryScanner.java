package dtool.source.scan;

import dtool.io.tree.FileTree;
import dtool.source.SourceFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoryScanner {

    private final String baseDirectory;
    private final FileTree fileTree;

    public DirectoryScanner(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.fileTree = scan();
    }

    // fucking try rsource crap
    @SuppressWarnings("all")
    private FileTree scan() {
        final Path path = Path.of(baseDirectory);
        final File file = path.toFile();

        if (file.exists() && file.isDirectory()) {
            try {
                List<SourceFile> rawTree = Files
                        .walk(path, FileVisitOption.FOLLOW_LINKS)
                        .filter(current -> current.toFile().isFile())
                        .map(current -> {
                            SourceFile sourceFile = new SourceFile(current.toString());
                            sourceFile.initialize();

                            return sourceFile;
                        }).collect(Collectors.toCollection(ArrayList::new));

                return new FileTree(rawTree);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new InvalidDirectoryException(baseDirectory);
    }

    public FileTree getFileTree() {
        return fileTree;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }
}
