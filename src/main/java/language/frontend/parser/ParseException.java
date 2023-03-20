package language.frontend.parser;

import dtool.logger.errors.LanguageException;

public class ParseException extends RuntimeException {

    private final LanguageException languageException;

    public ParseException(LanguageException languageException) {
        this.languageException = languageException;
    }

    public LanguageException getLanguageException() {
        return languageException;
    }
}
