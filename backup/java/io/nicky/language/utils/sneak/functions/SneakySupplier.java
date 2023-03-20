package language.utils.sneak.functions;

@FunctionalInterface
public interface SneakySupplier<T> {
    T get() throws Exception;
}
