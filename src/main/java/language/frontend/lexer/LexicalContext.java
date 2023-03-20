package language.frontend.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The type Lexical context.
 *
 * @param <T> the type parameter
 */
public class LexicalContext<T> {
    /**
     * The Rules.
     */
    protected final List<LexerRule<T>> rules;
    /**
     * The Whitespace.
     */
    protected final T whitespace;

    /**
     * Instantiates a new Lexical context.
     */
    public LexicalContext() {
        this.rules = new ArrayList<>();
        this.whitespace = null;
    }

    /**
     * Instantiates a new Lexical context.
     *
     * @param whitespace the whitespace
     */
    public LexicalContext(T whitespace) {
        this.rules = new ArrayList<>();
        this.whitespace = whitespace;
    }

    /**
     * Add rule lexical context.
     *
     * @param type     the type
     * @param consumer the consumer
     * @return the lexical context
     */
    public LexicalContext<T> addRule(T type, Consumer<LexerRule<T>> consumer) {
        LexerRule<T> rule = new LexerRule<>(type);
        consumer.accept(rule);
        rules.add(rule);
        return this;
    }

    /**
     * Parse list.
     *
     * @param input the input
     * @return the list
     */
    public List<LexicalToken<T>> parse(String input) {
        List<LexicalToken<T>> tokenList = new ArrayList<>();
        int last_length = 0;

        LexicalToken<T> lexicalToken;
        while ((lexicalToken = nextToken(input)) != null) {
            if (input.isEmpty() || (last_length == input.length())) break;

            if (lexicalToken.getType() != whitespace) {
                tokenList.add(lexicalToken);
            }

            last_length = input.length();
            input = input.substring(lexicalToken.getLength());
        }

        return tokenList;
    }

    /**
     * To immutable lexical context.
     *
     * @return the lexical context
     */
    public LexicalContext<T> toImmutable() {
        return new ImmutableContext<T>(this);
    }

    /**
     * Next token lexer token.
     *
     * @param input the input
     * @return the lexer token
     */
    public LexicalToken<T> nextToken(String input) {
        LexerRule<T> selectedRule = null;
        int longestRule = 1;
        for (LexerRule<T> rule : rules) {
            int length = rule.getMatchLength(input);

            if (length >= longestRule) {
                longestRule = length;
                selectedRule = rule;
            }

        }

        return selectedRule == null ? null :
                new LexicalToken<>(selectedRule.type, input.substring(0, longestRule));
    }

}