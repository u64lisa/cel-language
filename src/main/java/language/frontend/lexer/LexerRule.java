package language.frontend.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Lexer rule.
 *
 * @param <T> the type parameter
 */
@SuppressWarnings("all")
public class LexerRule<T> {
    /**
     * The Matches.
     */
    public final List<Pattern> matches;
    /**
     * The Type.
     */
    public final T type;

    /**
     * Instantiates a new Lexer rule.
     *
     * @param type the type
     */
    public LexerRule(T type) {
        this.matches = new ArrayList<>();
        this.type = type;
    }

    /**
     * Add string lexer rule.
     *
     * @param value the value
     * @return the lexer rule
     */
    public LexerRule<T> addString(String value) {
        this.matches.add(Pattern.compile(regexEscape(value)));
        return this;
    }

    /**
     * Add strings lexer rule.
     *
     * @param values the values
     * @return the lexer rule
     */
    public LexerRule<T> addStrings(String... values) {
        for (String value : values) {
            addString(value);
        }

        return this;
    }

    /**
     * Add regex lexer rule.
     *
     * @param regex the regex
     * @return the lexer rule
     */
    public LexerRule<T> addRegex(String regex) {
        this.matches.add(Pattern.compile(regex));
        return this;
    }

    /**
     * Add regexes lexer rule.
     *
     * @param regexes the regexes
     * @return the lexer rule
     */
    public LexerRule<T> addRegexes(String... regexes) {
        for (String regex : regexes) {
            addRegex(regex);
        }

        return this;
    }

    /**
     * Add singleline lexer rule.
     *
     * @param open  the open
     * @param close the close
     * @return the lexer rule
     */
    public LexerRule<T> addSingleline(String open, String close) {
        return addSingleline(open, "", close);
    }

    /**
     * Add singleline lexer rule.
     *
     * @param open   the open
     * @param escape the escape
     * @param close  the close
     * @return the lexer rule
     */
    public LexerRule<T> addSingleline(String open, String escape, String close) {
        return addDelimiter(open, escape, close, 0);
    }

    /**
     * Add multiline lexer rule.
     *
     * @param open  the open
     * @param close the close
     * @return the lexer rule
     */
    public LexerRule<T> addMultiline(String open, String close) {
        return addMultiline(open, "", close);
    }

    /**
     * Add multiline lexer rule.
     *
     * @param open   the open
     * @param escape the escape
     * @param close  the close
     * @return the lexer rule
     */
    public LexerRule<T> addMultiline(String open, String escape, String close) {
        return addDelimiter(open, escape, close, Pattern.DOTALL);
    }

    private LexerRule<T> addDelimiter(String open, String escape, String close, int flags) {
        String s = regexEscape(open);
        String c = regexEscape(close);

        String regex;
        if (escape.isEmpty()) {
            regex = s + ".*?" + c;
        } else {
            String e = regexEscape(escape);
            regex = s + "(?:" + e + "(?:" + e + "|" + c + "|(?!" + c + ").)|(?!" + e + "|" + c + ").)*" + c;
        }

        this.matches.add(Pattern.compile(regex, flags));
        return this;
    }

    /**
     * Gets match length.
     *
     * @param string the string
     * @return the match length
     */
    public int getMatchLength(String string) {
        int length = 0;
        for (Pattern pattern : matches) {
            Matcher matcher = pattern.matcher(string);
            if (matcher.lookingAt()) {
                length = Math.max(length, matcher.end());
            }
        }

        return length < 1 ? -1 : length;
    }

    /**
     * Regex escape string.
     *
     * @param string the string
     * @return the string
     */
    public String regexEscape(String string) {
        if (string == null) return null;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            switch (c) { // Normal escapes
                case '\0' -> {
                    sb.append("\\0");
                    continue;
                }
                case '\n' -> {
                    sb.append("\\n");
                    continue;
                }
                case '\r' -> {
                    sb.append("\\r");
                    continue;
                }
                case '\t' -> {
                    sb.append("\\t");
                    continue;
                }
                case '\\' -> {
                    sb.append("\\\\");
                    continue;
                }
                case '^', '$', '?', '|', '*', '/', '+', '.', '(', ')', '[', ']', '{', '}' -> {
                    sb.append("\\").append(c);
                    continue;
                }
            }

            if (c > 0xff) { // Unicode
                sb.append("\\u").append(toHexString(c, 4));
                continue;
            }

            if (Character.isISOControl(c)) { // Control character
                sb.append("\\x").append(toHexString(c, 2));
                continue;
            }

            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * To hex string string.
     *
     * @param value  the value
     * @param length the length
     * @return the string
     */
    public String toHexString(long value, int length) {
        if (length < 1)
            throw new IllegalArgumentException("The minimum length of the returned string cannot be less than one.");
        return String.format("%0" + length + "x", value);
    }

    @Override
    public String toString() {
        return "LexerRule{" +
                "matches=" + matches +
                ", type=" + type +
                '}';
    }
}