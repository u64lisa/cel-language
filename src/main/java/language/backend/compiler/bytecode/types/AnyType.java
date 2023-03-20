package language.backend.compiler.bytecode.types;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

public class AnyType extends PrimitiveType {
    static final AnyType INSTANCE = new AnyType();

    private AnyType() {
        super("any");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        return INSTANCE;
    }

    @Override
    protected Type operation(TokenType operation) {
        return INSTANCE;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.ANY};
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Type;
    }
}
