package language.frontend.lexer;

/**
 * The type Lexer token.
 *
 * @param <T> the type parameter
 */
public class LexicalToken<T> {
    private final T type;
    private final String content;
    private final int length;

    /**
     * Instantiates a new Lexer token.
     *
     * @param type    the type
     * @param content the content
     */
    public LexicalToken(T type, String content) {
        this.length = content.length();
        this.content = content;
        this.type = type;

    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public T getType() {
        return type;
    }

    /**
     * Gets content.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets length.
     *
     * @return the length
     */
    public int getLength() {
        return length;
    }
}
    