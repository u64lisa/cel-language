package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

import java.util.Arrays;

import static language.backend.compiler.bytecode.types.primitives.numbers.I32Type.VALID_OPS;

public final class I128Type extends PrimitiveType {
    public static final I128Type INSTANCE = new I128Type();

    private I128Type() {
        super("i128");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (!other.matches(Types.INT, Types.DOUBLE, Types.FLOAT, Types.LONG, Types.BYTE)) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.LONG;
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
        return o instanceof F32Type || o instanceof I32Type || o instanceof I128Type ||
                o instanceof I64Type || o instanceof I16Type || o instanceof I8Type;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.LONG};
    }
}
