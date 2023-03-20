package language.backend.compiler.bytecode.types.objects;

import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.values.Value;

import java.util.*;

public class EnumType extends Type {
    public final Map<String, EnumChildType> children;
    public EnumType(String name, EnumChildType[] children) {
        super(name);
        this.children = new HashMap<>(children.length);
        for (EnumChildType child : children) {
            this.children.put(child.name, child);
        }
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
        return null;
    }

    @Override
    public Type access(String name) {
        return children.get(name);
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.ENUM);
        Value.addAllString(list, name);
        list.add(children.size());
        for (EnumChildType child : children.values()) {
            list.addAll(child.dumpList());
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        EnumChildType[] newChildren = new EnumChildType[children.size()];
        int i = 0;
        for (EnumChildType child : children.values()) {
            newChildren[i++] = (EnumChildType) child.applyGenerics(generics);
        }
        return new EnumType(name, newChildren);
    }
}
