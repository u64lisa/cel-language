package dtool.source;

public class EmptySourceFileException extends RuntimeException {

    public EmptySourceFileException(String message) {
        super("Provided file does not exist: " + message);
    }
}
