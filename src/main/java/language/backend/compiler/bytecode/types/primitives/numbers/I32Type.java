package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

import java.util.Arrays;

public class I32Type extends PrimitiveType {
    public static final I32Type INSTANCE = new I32Type();

    static final TokenType[] VALID_OPS = {
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.STAR,
            TokenType.SLASH,
            TokenType.CARET,
            TokenType.PERCENT,
            TokenType.RIGHT_ANGLE,
            TokenType.LEFT_ANGLE,
            TokenType.GREATER_EQUALS,
            TokenType.LESS_EQUALS,
            TokenType.TILDE_AMPERSAND,
            TokenType.TILDE_PIPE,
            TokenType.TILDE_CARET,
            TokenType.LEFT_TILDE_ARROW,
            TokenType.TILDE_TILDE,
            TokenType.RIGHT_TILDE_ARROW,
            TokenType.PLUS_PLUS,
            TokenType.MINUS_MINUS,
    };

    private I32Type() {
        super("i32");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (!other.matches(Types.INT, Types.DOUBLE, Types.FLOAT, Types.LONG, Types.BYTE)) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return other == Types.FLOAT ? Types.FLOAT : Types.INT;
        }
        return null;
    }

    @Override
    public Type operation(TokenType operation) {
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.INT;
        }
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.INT};
    }
}
