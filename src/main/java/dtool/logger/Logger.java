package dtool.logger;

import language.backend.compiler.bytecode.values.Value;
import dtool.logger.errors.LanguageException;

public interface Logger {

    String toString(Object text, boolean stringInner);

    void out(Object text);

    void warn(Object text);


    void fail(final String file, final String source, LanguageException languageException);

    void debug(Value val);

    void printObject(Object text);

    void debug(String format);

    boolean isDebugging();

    String readLine();
}
