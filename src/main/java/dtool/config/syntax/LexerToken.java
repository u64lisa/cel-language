package dtool.config.syntax;

public class LexerToken<T> {
    public final T type;
    public final String content;
    public final int length;

    public LexerToken(T type, String content) {
        this.length = content.length();
        this.content = content;
        this.type = type;
    }
}