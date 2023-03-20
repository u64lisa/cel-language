package language.utils.arguments;

public interface IOption<T> {
    String name();

    Object value();

    Object defaultValue();

    boolean isPresent();

    String[] getAliases();
}
