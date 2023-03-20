/*
 * see license.txt
 */
package assembler.exception;

public class EvalException extends RuntimeException {
    private static final long serialVersionUID = -4213535526765610619L;

    public EvalException(String message) {
        super(message);
    }

    public EvalException(String message, Throwable cause) {
        super(message, cause);
    }
}
