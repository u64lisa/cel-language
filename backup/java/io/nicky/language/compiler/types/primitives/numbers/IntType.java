package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

import java.util.Arrays;

public class IntType extends PrimitiveType {
    public static final IntType INSTANCE = new IntType();

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

    private IntType() {
        super("int");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (other != Types.INT && other != Types.DOUBLE && other != Types.FLOAT && other != Types.LONG) {
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
