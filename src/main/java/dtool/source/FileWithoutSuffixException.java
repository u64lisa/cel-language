package dtool.source;

public class FileWithoutSuffixException extends IllegalArgumentException {

    public FileWithoutSuffixException(String s) {
        super("found file without suffix in source tree: " + s);
    }
}
