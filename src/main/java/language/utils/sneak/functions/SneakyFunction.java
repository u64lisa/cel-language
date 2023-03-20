package language.utils.sneak.functions;

@FunctionalInterface
public interface SneakyFunction<T, R> {
    R apply(T var1) throws Exception;
}
