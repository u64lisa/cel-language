package language.backend.compiler.bytecode.types.objects;

import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReferenceType extends Type {
    public Type ref;

    public ReferenceType(Type ref) {
        super(null);
        updateRef(ref);
    }

    private void updateRef(Type ref) {
        this.name = "Ref[" + ref.name + "]";
        this.ref = ref;
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.FAT_ARROW) {
            updateRef(other);
            return Types.VOID;
        }
        return null;
    }

    @Override
    public Type isCompatible(TokenType operation, Type other) {
        return this.ref.isCompatible(operation, other);
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
        return null;
    }

    @Override
    public Type accessInternal(String name) {
        return null;
    }

    @Override
    public int[] dump() {
        List<Integer> list = new ArrayList<>();
        list.add(TypeCodes.REFERENCE);
        list.addAll(ref.dumpList());
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Type applyGenerics(Map<Type, Type> generics) {
        return new ReferenceType(ref.applyGenerics(generics));
    }

    public Type getRef() {
        return ref;
    }

}
