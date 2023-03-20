package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

import java.util.Arrays;

import static language.backend.compiler.bytecode.types.primitives.numbers.IntType.VALID_OPS;

public final class ShortType extends PrimitiveType {

    public static final ShortType INSTANCE = new ShortType();

    private ShortType() {
        super("short");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (other != Types.SHORT) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.SHORT;
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
        return o instanceof ShortType || o instanceof  ByteType;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.SHORT};
    }
}
