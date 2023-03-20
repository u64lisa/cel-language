package language.backend.compiler.bytecode.types;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

public class VoidType extends PrimitiveType {
    static final VoidType INSTANCE = new VoidType();

    private VoidType() {
        super("void");
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
    public int[] dump() {
        return new int[]{TypeCodes.VOID};
    }
}
