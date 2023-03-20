package dtool.config.syntax;


import dtool.config.syntax.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LexerRule<T> {
    public final List<Pattern> matches;
    public final T type;

    public LexerRule(T type) {
        this.matches = new ArrayList<>();
        this.type = type;
    }

    public LexerRule<T> addString(String value) {
        this.matches.add(Pattern.compile(StringUtils.regexEscape(value)));
        return this;
    }

    public LexerRule<T> addStrings(String... values) {
        for (String value : values) {
            addString(value);
        }

        return this;
    }

    public LexerRule<T> addRegex(String regex) {
        this.matches.add(Pattern.compile(regex));
        return this;
    }

    public LexerRule<T> addRegexes(String... regexes) {
        for (String regex : regexes) {
            addRegex(regex);
        }

        return this;
    }

    public LexerRule<T> addSingleline(String open, String close) {
        return addSingleline(open, "", close);
    }

    public LexerRule<T> addSingleline(String open, String escape, String close) {
        return addDelimiter(open, escape, close, 0);
    }

    public LexerRule<T> addMultiline(String open, String close) {
        return addMultiline(open, "", close);
    }

    public LexerRule<T> addMultiline(String open, String escape, String close) {
        return addDelimiter(open, escape, close, Pattern.DOTALL);
    }

    private LexerRule<T> addDelimiter(String open, String escape, String close, int flags) {
        String s = StringUtils.regexEscape(open);
        String c = StringUtils.regexEscape(close);

        String regex;
        if (escape.isEmpty()) {
            regex = s + ".*?" + c;
        } else {
            String e = StringUtils.regexEscape(escape);
            regex = s + "(?:" + e + "(?:" + e + "|" + c + "|(?!" + c + ").)|(?!" + e + "|" + c + ").)*" + c;
        }

        this.matches.add(Pattern.compile(regex, flags));
        return this;
    }

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

	public List<Pattern> getMatches() {
		return matches;
	}

}