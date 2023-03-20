package language.backend.compiler.bytecode.types.primitives.primitive;

import language.backend.compiler.bytecode.types.Type;

import java.util.Map;

public abstract class PrimitiveType extends Type {
    public PrimitiveType(String name) {
        super(name);
    }

    @Override
    public Type call(final Type[] args, final Type[] generics) {
        return null;
    }

    @Override
    public Type access(final String name) {
        return null;
    }

    @Override
    public Type accessInternal(final String name) {
        return null;
    }

    @Override
    public Type applyGenerics(final Map<Type, Type> generics) {
        return this;
    }

}
