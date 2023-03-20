package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

import java.util.Arrays;

import static language.backend.compiler.bytecode.types.primitives.numbers.IntType.VALID_OPS;

public class FloatType extends PrimitiveType {
    public static final FloatType INSTANCE = new FloatType();

    private FloatType() {
        super("float");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (other != Types.INT && other != Types.DOUBLE && other != Types.FLOAT && other != Types.LONG) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.FLOAT;
        }
        return null;
    }

    @Override
    public Type operation(TokenType operation) {
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return this;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        // An int can be a float, but a float can't be an int.
        return o instanceof FloatType || o instanceof IntType || o instanceof ShortType || o instanceof ByteType;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.FLOAT};
    }
}