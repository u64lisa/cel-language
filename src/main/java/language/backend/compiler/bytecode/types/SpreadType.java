package language.backend.compiler.bytecode.types;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

public class SpreadType extends PrimitiveType {
    static final SpreadType INSTANCE = new SpreadType();

    private SpreadType() {
        super("spread");
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
