package language.backend.compiler.bytecode.types.primitives.collection;

import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveType;
import language.frontend.lexer.token.TokenType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.TypeCodes;
import language.backend.compiler.bytecode.types.Types;

public class MapType extends PrimitiveType {
    public static final MapType INSTANCE = new MapType();

    private MapType() {
        super("map");
    }

    @Override
    protected Type operation(TokenType operation, Type other) {
        if (operation == TokenType.LEFT_BRACKET || operation == TokenType.DOT) {
            return Types.ANY;
        }
        else if (operation == TokenType.PLUS) {
            return other == Types.MAP ? Types.MAP : null;
        }
        return null;
    }

    @Override
    protected Type operation(TokenType operation) {
        return null;
    }

    @Override
    public int[] dump() {
        return new int[]{TypeCodes.MAP};
    }
}
