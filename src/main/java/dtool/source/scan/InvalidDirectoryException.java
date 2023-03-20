package dtool.source.scan;

public class InvalidDirectoryException extends RuntimeException {
    public InvalidDirectoryException(String message) {
        super("Can't scan directory in path! may be in existing or not a directory: " + message);
    }
}
