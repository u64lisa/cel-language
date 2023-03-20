package language.utils;

public class WrappedCast {

    public static  <T> T cast(final Object value) {
        try {
            return (T) value;
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
