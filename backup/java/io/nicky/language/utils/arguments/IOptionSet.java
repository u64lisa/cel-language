package language.utils.arguments;

public interface IOptionSet {
    boolean isPresent(String name);

    Option getOption(String name);

    String getValueOfObject(String name);

    boolean getState(String name);
}
