package language.utils.sneak.functions;

@FunctionalInterface
public interface SneakyBiPredicate<T, U> {
    boolean test(T var1, U var2) throws Exception;
}
