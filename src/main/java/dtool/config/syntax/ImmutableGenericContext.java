package dtool.config.syntax;

import java.util.function.Consumer;

public class ImmutableGenericContext<T> extends GenericLexerContext<T> {

    public ImmutableGenericContext(GenericLexerContext<T> context) {
        this.rules.addAll(context.rules);
    }

    @Override
    public GenericLexerContext<T> addRule(T type, Consumer<LexerRule<T>> consumer) {
        throw new UnsupportedOperationException();
    }

}