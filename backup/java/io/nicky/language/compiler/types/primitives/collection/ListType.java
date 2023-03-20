package language.backend.compiler.bytecode.types.primitives.collection;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

public class ListType extends PrimitiveType {
    public static final ListType INSTANCE = new ListType();

    private ListType() {
        super("list");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.DOT || operation == TokenType.LEFT_BRACKET) {
            return Types.FLOAT.equals(other) ? Types.ANY : null;
        }
        else if (operation == TokenType.PLUS) {
            return other == Types.LIST ? Types.LIST : null;
        }
        else if (operation == TokenType.SLASH || operation == TokenType.PERCENT) {
            return Types.LIST;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.LIST};
    }
}
