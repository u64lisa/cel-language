package dtool.source;

public class UnknownFileSuffixException extends IllegalArgumentException {

    public UnknownFileSuffixException(String s) {
        super("found unknown file suffix in source tree: " + s);
    }

}
