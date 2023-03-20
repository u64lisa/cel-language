package language.backend.compiler.bytecode.types.primitives.other;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

public class BytesType extends PrimitiveType {
    public  static final BytesType INSTANCE = new BytesType();

    private BytesType() {
        super("bytearray");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.LEFT_BRACKET) {
            return Types.INT;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.BYTES};
    }
}
