package language.backend.compiler.bytecode.types;

import language.backend.compiler.bytecode.ChunkCode;
import language.backend.compiler.bytecode.types.objects.ReferenceType;
import language.frontend.lexer.token.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Type {

    public String name;

    public Type(final String name) {
        this.name = name;
    }

    public boolean callable() {
        return false;
    }

    // Binary operations
    // If it returns null, operation is incompatible
    protected abstract Type operation(final TokenType operation, final Type other);

    public Type isCompatible(final TokenType operation, final Type other) {
        boolean stringCompatibility = other == Types.STRING || this == Types.STRING;

        if (operation == TokenType.EQUAL_EQUAL || operation == TokenType.BANG_EQUAL) {
            return Types.BOOL;
        }
        else if (other instanceof AnyType) {
            return Types.ANY;
        }
        else if (operation == TokenType.COLON) {
            return this instanceof VoidType ? other : this;
        }

        final Type operated = operation(operation, other);

        if (operated == null && stringCompatibility)
            return Types.STRING;

        return operated;
    }

    // Unary operations
    // If it returns null, operation is incompatible
    protected abstract Type operation(TokenType operation);
    public Type isCompatible(TokenType operation) {
        return operation(operation);
    }

    public abstract Type call(final Type[] arguments, final Type[] generics);

    public abstract Type access(final String name);
    public abstract Type accessInternal(final String name);

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    public int[] compile() {
        int[] dump = dump();
        int[] result = new int[dump.length + 1];
        result[0] = ChunkCode.Type;
        System.arraycopy(dump, 0, result, 1, dump.length);
        return result;
    }

    public abstract int[] dump();

    public abstract Type applyGenerics(final Map<Type, Type> generics);

    public List<Integer> dumpList() {
        return Arrays.stream(compile()).boxed().collect(Collectors.toList());
    }

    public boolean matches(final Type... other) {
        boolean matches = false;
        if (this instanceof ReferenceType referenceType) {
            Type reference = referenceType.ref;

            for (Type type : other) {
                if (type == reference) {
                    matches = true;
                    break;
                }
            }

        } else {
            for (Type type : other) {
                if (type == this) {
                    matches = true;
                    break;
                }
            }
        }

        return matches;
    }

    public String getName() {
        return name;
    }
}
