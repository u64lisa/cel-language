package dtool.logger;

import language.backend.compiler.bytecode.values.Value;
import dtool.logger.errors.LanguageException;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ImplLogger implements Logger {

    private static final Logger instance = new ImplLogger();

    private final Scanner scanner = new Scanner(System.in);

    private final boolean debug;
    private boolean failed = false;

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
    public void fail(LanguageException languageException) {
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


        final StringBuilder errorMessage = appendFormattedDetails(new StringBuilder("\n")
                .append("=".repeat(70))
                .append("\n")
                .append("Error while parsing file!")
                .append("\n")
                .append("\n")
                .append("file:  ")
                .append("\"")
                .append(languageException.positionStart()
                        .getFile().replace("/./", "/"))
                .append("\"")
                .append("\n")
                .append("\n")
                .append("cause:  ")
                .append(languageException.errorName())
                .append("\n"), languageException)
                .append("\n")
                .append("position:  line= ")
                .append(languageException.positionStart().getLine() + 1)
                .append(", column= ")
                .append(languageException.positionStart().getCurrentColumn())
                .append(", file position= (")
                .append(languageException.positionStart().getLine())
                .append(":")
                .append(languageException.positionStart().getCurrentColumn())
                .append(")")
                .append("\n")
                .append("\n")
                .append(highlightError(languageException));

        System.err.println(errorMessage);

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

    private String highlightError(final LanguageException languageException) {
        final String text = languageException.positionStart().getSource();

        final StringBuilder stringBuilder = new StringBuilder();
        int xStart = Math.max(0, text.lastIndexOf("\n", languageException.positionStart().getIndex()));
        int xEnd = text.indexOf("\n", xStart + 1);

        if (xEnd < 0) xEnd = text.length();

        int lineCount = languageException.positionEnd().getLine() - languageException.positionStart().getLine() + 1;
        int offset = 0;
        int colonStart, colonEnd, distance;


        int currentErrorLine = languageException.positionStart().getLine() + 1;

        stringBuilder.append("-".repeat(70)).append("\n");
        stringBuilder.append("    |").append("\n");

        for (int index = 0; index < lineCount; index++) {
            String line = text.substring(xStart, xEnd);

            colonStart = index == 0 ? languageException.positionStart().getCurrentColumn() : nonWhitespace(line);
            colonEnd = index == lineCount - 1 ? languageException.positionEnd().getCurrentColumn() : line.length();
            distance = colonEnd - colonStart;

            String group;
            if (distance >= 2) {
                group = "^".repeat(distance + 2);
            } else {
                group = "^";
            }

            String lineNumber = currentErrorLine + "";
            lineNumber += " ".repeat(4 - lineNumber.length()) + "|";

            stringBuilder
                    .append(lineNumber)
                    .append(line.replace("\n", ""))
                    .append("\n");

            int spacing = whitespaceInfront(line);

            int underline = line.length() - 1 - spacing;

            if (underline != distance + 2)
                stringBuilder
                        .append("    |").append(" ".repeat(Math.max(spacing, 0)))
                        .append(("~").repeat(underline)).append("\n");

            stringBuilder
                    .append("E   |")
                    .append(" ".repeat(Math.max(0, colonStart + offset)))
                    .append(group)
                    .append("\n");


            xStart = xEnd;
            xEnd = text.indexOf("\n", xStart + 1);

            if (colonEnd < 0)
                xEnd = text.length();

            currentErrorLine++;
        }

        stringBuilder.append("    |");
        return stringBuilder.toString()
                .replace("\t", "");
    }

    private int whitespaceInfront(final String text) {
        int count = -1;
        for (char c : text.toCharArray()) {
            if (c == ' ' || c == '\n')
                ++count;

            else
                return count;
        }
        return count;
    }

    private int nonWhitespace(String string) {
        char[] characters = string.toCharArray();
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isWhitespace(characters[i])) {
                return i;
            }
        }
        return 0;
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
