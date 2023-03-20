package language.backend.compiler.bytecode.types.primitives.other;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

public class StringType extends PrimitiveType {
    public static final StringType INSTANCE = new StringType();

    private StringType() {
        super("String");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.PLUS) {
            return Types.STRING;
        }
        else if (operation == TokenType.STAR && other == Types.INT) {
            return Types.STRING;
        }
        else if (operation == TokenType.LEFT_BRACKET && other == Types.INT) {
            return Types.STRING;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.STRING};
    }
}
