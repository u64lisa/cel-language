package dtool.logger;

import language.backend.compiler.bytecode.values.Value;
import dtool.logger.errors.ExceptionHighlighter;
import dtool.logger.errors.LanguageException;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ImplLogger implements Logger {

    private static final Logger instance = new ImplLogger();

    private final Scanner scanner = new Scanner(System.in);

    private final boolean debug;
    private boolean failed = false;

    private final ExceptionHighlighter highlighter = new ExceptionHighlighter();

    ImplLogger() {
        debug = System.getProperty("lang.debug", "false").equals("true");
    }


    @Override
    public void out(Object text) {
        System.out.print(toString(text, false));
        System.out.flush();
    }

    @Override
    public void warn(Object text) {
        System.out.println(toString(text, false));
    }

    @Override
    public void fail(final String file, final String source, LanguageException languageException) {
        if (failed)
            return;

        failed = true;

        if (languageException.type().equals(LanguageException.Type.COMPILER)) {
            final StringBuilder errorMessage = appendFormattedDetails(new StringBuilder("\n")
                    .append("=".repeat(70))
                    .append("\n")
                    .append("Error while compiling file!")
                    .append("\n")
                    .append("\n")
                    .append("cause:  ")
                    .append(languageException.errorName())
                    .append("\n"), languageException);

            System.err.println(errorMessage);

            System.exit(0);
            return;
        }


        System.err.println(highlighter.createFullError(
                new PositionedError(languageException, file, source)));

        System.exit(0);
    }

    private StringBuilder appendFormattedDetails(final StringBuilder stringBuilder, final LanguageException languageException) {
        stringBuilder.append("details:  ");
        for (int i = 0; i < languageException.details().length; i++) {
            if (i == 0) {
                stringBuilder.append(languageException.details()[i]).append("\n");
                continue;
            }
            stringBuilder.append(" ".repeat(10)).append(languageException.details()[i]).append("\n");
        }
        return stringBuilder;
    }

    @Override
    public void debug(Value val) {
        if (debug) {
            System.out.println(toString(val, false));
        }
    }

    @Override
    public void printObject(Object text) {
        System.out.println(toString(text, false));
    }

    @Override
    public void debug(String format) {
        if (debug) {
            System.out.println(toString(format, false));
        }
    }

    @Override
    public boolean isDebugging() {
        return this.debug;
    }

    @Override
    public String readLine() {
        return scanner.nextLine();
    }

    @Override
    public String toString(Object text, boolean stringInner) {
        if (text instanceof Value val) {
            if (val.isList) {
                StringBuilder sb = new StringBuilder();
                List<Value> l = val.asList();

                for (int i = 0; i < l.size(); i++)
                    if (i >= 5 && i < l.size() - 5) {
                        if (i == 5 + 1) sb.append("..., ");
                    } else sb.append(toString(l.get(i), true)).append(", ");

                return "[ " + sb + "len=" + l.size() + " ]";
            } else if (val.isMap) {
                StringBuilder sb = new StringBuilder();
                Map<Value, Value> d = val.asMap();

                Value[] keys = d.keySet().toArray(new Value[0]);
                for (int i = 0; i < keys.length; i++)
                    if (i >= 5 && i < keys.length - 5) {
                        if (i == 5 + 1) sb.append("..., ");
                    } else sb.append(toString(keys[i], true)).append(": ")
                            .append(toString(d.get(keys[i]), true)).append(", ");

                return "{ " + sb + "len=" + keys.length + " }";
            } else if (val.isNull) {
                return "null";
            } else if (val.isString && stringInner) {
                return "\"" + val.asString() + "\"";
            }
            return val.asString();
        }
        return text.toString();
    }

    public static Logger getInstance() {
        return instance;
    }
}
