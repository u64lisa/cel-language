package language.utils.sneak.functions;

@FunctionalInterface
public interface SneakyBiFunction<T, U, R> {
    R apply(T var1, U var2) throws Exception;
}
