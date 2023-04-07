package language.backend.compiler.bytecode.types.primitives.other;

import language.backend.compiler.bytecode.types.objects.ReferenceType;
import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;

import java.util.Arrays;

public class BooleanType extends PrimitiveType {
    public static final BooleanType INSTANCE = new BooleanType();

    static final TokenType[] VALID_OPS = {
            TokenType.AMPERSAND,
            TokenType.PIPE,
            TokenType.BANG
    };

    private BooleanType() {
        super("bool");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {

        if (!(other instanceof BooleanType) && !(other instanceof ReferenceType referenceType
                && referenceType.getRef() instanceof BooleanType)) {
            return null;
        }

        if (Arrays.asList(VALID_OPS).contains(operation)) {

            return INSTANCE;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        if (Arrays.asList(VALID_OPS).contains(operation)) {
            return INSTANCE;
        }
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.BOOL};
    }
}
