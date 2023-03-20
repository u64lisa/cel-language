package dtool.io;

import java.nio.file.Path;

// folder are important!
public class CantCreateFolderException extends RuntimeException {

    public CantCreateFolderException(final Path folder) {
        super("Failed to create folder: " + folder.toString());
    }
}
