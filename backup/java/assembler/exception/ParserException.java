/*
 * see license.txt
 */
package assembler.exception;

public class ParserException extends RuntimeException {
    private static final long serialVersionUID = 2787078153119100929L;

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
