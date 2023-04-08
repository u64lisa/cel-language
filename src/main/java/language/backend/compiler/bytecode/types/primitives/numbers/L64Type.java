package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

import java.util.Arrays;

import static language.backend.compiler.bytecode.types.primitives.numbers.I32Type.VALID_OPS;

public final class L64Type extends PrimitiveType {
    public static final L64Type INSTANCE = new L64Type();

    private L64Type() {
        super("l64");
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
        return o instanceof F64Type || o instanceof I32Type || o instanceof L64Type ||
                o instanceof I64Type || o instanceof I16Type || o instanceof I8Type;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.LONG};
    }
}
