package language.backend.compiler.bytecode.types.objects;

import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;

import java.util.Map;

public class MethodType extends Type {
    public final ClassObjectType classObjectType;
    public final Map<Type, Type> generics;

    public MethodType(ClassObjectType inner, Map<Type, Type> generics) {
        super("method");
        this.classObjectType = inner;
        this.generics = generics;
    }

    @Override
    public boolean callable() {
        return true;
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        return new MethodType((ClassObjectType) classObjectType.applyGenerics(generics), generics);
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public Type call(Type[] arguments, Type[] generics) {
        return classObjectType.call(arguments, generics, this.generics);
    }

    @Override
    public Type access(String name) {
        return null;
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @SuppressWarnings("EQUALsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return classObjectType.equals(o);
    }

    @Override
    public int[] dump() {
        return classObjectType.dump();
    }
}
