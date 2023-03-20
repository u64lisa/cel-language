package io.nicky.language.workspace.source;

public interface Filter<T> {

    boolean passes(T value);

}
