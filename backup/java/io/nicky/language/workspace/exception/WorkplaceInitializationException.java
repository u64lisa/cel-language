package io.nicky.language.workspace.exception;

public class WorkplaceInitializationException extends RuntimeException {

    public WorkplaceInitializationException() {
    }

    public WorkplaceInitializationException(String message) {
        super(message);
    }

    public WorkplaceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkplaceInitializationException(Throwable cause) {
        super(cause);
    }

    public WorkplaceInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
