package language.utils.sneak;

public class SneakyException extends RuntimeException {
    public SneakyException(Exception exception) {
        super(exception);
    }
}
