package language.utils.sneak.functions;

@FunctionalInterface
public interface SneakyConsumer<T> {
    void accept(T var1) throws Exception;
}
