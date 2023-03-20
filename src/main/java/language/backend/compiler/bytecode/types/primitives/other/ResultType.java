package language.backend.compiler.bytecode.types.primitives.other;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;

public class ResultType extends PrimitiveType {
    public static final ResultType INSTANCE = new ResultType();

    private ResultType() {
        super("catcher");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.RESULT};
    }
}
