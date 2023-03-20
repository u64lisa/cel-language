package dtool.config.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GenericLexerContext<T> {
    protected final List<LexerRule<T>> rules;
    protected final T whitespace;

    public GenericLexerContext() {
        this.rules = new ArrayList<>();
        this.whitespace = null;
    }

    public GenericLexerContext(T whitespace) {
        this.rules = new ArrayList<>();
        this.whitespace = whitespace;
    }

    public GenericLexerContext<T> addRule(T type, Consumer<LexerRule<T>> consumer) {
        LexerRule<T> rule = new LexerRule<T>(type);
        consumer.accept(rule);
        rules.add(rule);
        return this;
    }

    public List<LexerToken<T>> parse(String input) {
        List<LexerToken<T>> tokenList = new ArrayList<>();
        int last_length = 0;

        LexerToken<T> lexerToken;
        while ((lexerToken = nextToken(input)) != null) {
            if (input.isEmpty() || (last_length == input.length())) break;

            if (lexerToken.type != whitespace) {
                tokenList.add(lexerToken);
            }

            last_length = input.length();
            input = input.substring(lexerToken.length);
        }

        return tokenList;
    }

    public GenericLexerContext<T> toImmutable() {
        return new ImmutableGenericContext<T>(this);
    }

    public LexerToken <T>nextToken(String input) {
        LexerRule<T> selectedRule = null;
        int longestRule = 1;
        for (LexerRule<T> rule : rules) {
            int length = rule.getMatchLength(input);

            if (length >= longestRule) {
                longestRule = length;
                selectedRule = rule;
            }
        }

        return selectedRule == null ? null : new LexerToken<T>(selectedRule.type, input.substring(0, longestRule));
    }

    public List<LexerRule<T>> getRules() {
        return rules;
    }
}
