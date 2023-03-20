package dtool.utils;

public class DynamicOptional<T> {

    private T value;

    public boolean isPresent() {
        return value != null;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
