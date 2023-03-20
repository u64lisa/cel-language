package language.frontend.lexer;

import java.util.function.Consumer;

/**
 * The type Immutable generic context.
 *
 * @param <T> the type parameter
 */
public class ImmutableContext<T> extends LexicalContext<T> {
    /**
     * Instantiates a new Immutable generic context.
     *
     * @param context the context
     */
    public ImmutableContext(LexicalContext<T> context) {
        this.rules.addAll(context.rules);
    }

    @Override
    public LexicalContext<T> addRule(T type, Consumer<LexerRule<T>> consumer) {
        throw new UnsupportedOperationException();
    }
}