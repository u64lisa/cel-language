package dtool.logger;

import dtool.logger.errors.LanguageException;

public record PositionedError(LanguageException languageException, String file, String source) {

}
