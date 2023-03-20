package language.utils.sneak.functions;

@FunctionalInterface
public interface SneakyPredicate<T> {
    boolean test(T var1) throws Exception;
}
