package language.backend.compiler.bytecode.types.objects;

import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

import java.util.*;

public class InstanceType extends Type {
    private final ClassType parent;
    private final Type[] generics;
    private final Map<Type, Type> genericMap;

    public InstanceType(ClassType parent, Type[] generics) {
        super(parent.identifier);
        this.parent = parent;
        this.generics = generics;
        this.genericMap = new HashMap<>();
        for (int i = 0; i < generics.length; i++) {
            genericMap.put(parent.generics[i], generics[i]);
        }
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        String name;
        switch (operation) {
            case PLUS:
                name = "add";
                break;

            case MINUS:
                name = "sub";
                break;

            case STAR:
                name = "mul";
                break;

            case SLASH:
                name = "div";
                break;

            case PERCENT:
                name = "mod";
                break;

            case CARET:
                name = "fastpow";
                break;

            case DOT:
                name = "get";
                break;

            case LEFT_BRACKET:
                name = "bracket";
                break;

            case EQUAL_EQUAL:
                name = "eq";
                break;

            case BANG_EQUAL:
                name = "ne";
                break;

            case LEFT_ANGLE:
                name = "lt";
                break;

            case LESS_EQUALS:
                name = "lte";
                break;

            default:
                return null;
        }
        return parent.getOperator(name);
    }

    @Override
    public Type isCompatible(TokenType operation, Type other) {
        Type overload = wrapGenerics(operation(operation, other));
        if (overload != null) {
            return overload.call(new Type[]{ other }, new Type[0]);
        }
        if (operation == TokenType.EQUAL_EQUAL || operation == TokenType.BANG_EQUAL) {
            return Types.BOOL;
        }
        else if (operation == TokenType.COLON) {
            return this;
        }
        return null;
    }
    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public Type call(Type[] arguments, Type[] generics) {
        return null;
    }

    public Type wrapGenerics(Type type) {
        return type.applyGenerics(genericMap);
    }

    @Override
    public Type access(String name) {
        return wrapGenerics(parent.get(name, false));
    }

    @Override
    public Type accessInternal(String name) {
        return wrapGenerics(parent.get(name, true));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (generics.length > 0) {
            sb.append('<');
            sb.append(generics[0]);
            for (int i = 1; i < generics.length; i++) {
                sb.append(',');
                sb.append(generics[i]);
            }
            sb.append('>');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof InstanceType) {
            InstanceType otherType = (InstanceType)other;
            return otherType.parent.equals(parent) && Arrays.equals(otherType.generics, generics);
        }
        return false;
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.INSTANCE);
        list.addAll(parent.dumpList());
        list.add(generics.length);
        for (Type generic : generics) {
            list.addAll(generic.dumpList());
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        Type[] newGenerics = new Type[this.generics.length];
        boolean swapped = false;
        for (int i = 0; i < this.generics.length; i++) {
            newGenerics[i] = this.generics[i].applyGenerics(generics);
            swapped |= !newGenerics[i].equals(this.generics[i]);
        }
        return new InstanceType(swapped ? (ClassType) parent.applyGenerics(generics) : parent, newGenerics);
    }
}
