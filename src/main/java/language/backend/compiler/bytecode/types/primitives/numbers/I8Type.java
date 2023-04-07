package language.backend.compiler.bytecode.types.primitives.numbers;

import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;

import java.util.Arrays;

import static language.backend.compiler.bytecode.types.primitives.numbers.I32Type.VALID_OPS;

public final class I8Type extends PrimitiveType {

    public static final I8Type INSTANCE = new I8Type();

    private I8Type() {
        super("i8");
    }

    @Override
    public Type operation(TokenType operation, Type other) {
        if (other != Types.BYTE) {
            return null;
        }
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return Types.BYTE;
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
        return o instanceof I8Type || o instanceof I16Type;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.BYTE};
    }
}

