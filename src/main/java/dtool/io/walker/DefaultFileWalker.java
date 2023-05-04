package dtool.io.walker;

import dtool.io.CantCreateFolderException;
import dtool.io.ProjectFolder;

import java.io.File;
import java.nio.file.Path;

public class DefaultFileWalker extends FileWalker {

    @Override
    public void walk(ProjectFolder folder, String[] structure) {
        final Path root = folder.getAsPath();
        final File rootFile = root.toFile();

        checkSingleton(root);

        for (String subFolder : structure) {
            final Path subPath = Path.of(root.toString(), subFolder);
            this.checkSingleton(subPath);
        }
    }

    public void checkSingleton(final Path file) {
        final File ioFile = file.toFile();
        if (!ioFile.exists() && !ioFile.mkdirs())
            throw new CantCreateFolderException(file);
    }

}
