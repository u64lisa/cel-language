package language.backend.compiler.asm.types;

import language.backend.compiler.asm.inst.Parameter;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;

import java.util.Arrays;

public class ASMTypeLookup {

    public static final ValueType
            I8 = new ValueType("i8", 8, 0, 0),
            I16 = new ValueType("i16", 16, 0, 0),
            I32 = new ValueType("i32", 32, 0, 0),
            I64 = new ValueType("i64", 64, 0, 0);

    public static final ValueType
            U8 = new ValueType("u8", 8, 0, ValueType.UNSIGNED),
            U16 = new ValueType("u16", 16, 0, ValueType.UNSIGNED),
            U32 = new ValueType("u32", 32, 0, ValueType.UNSIGNED),
            U64 = new ValueType("u64", 64, 0, ValueType.UNSIGNED);

    public static final ValueType
            F32 = new ValueType("f32", 32, 0, ValueType.FLOATING);

    public static final ValueType
            L64 = new ValueType("l64", 64, 0, ValueType.LONG);

    public static final ValueType[] TYPES = {
            I8, I16, I32, I64, U8, U16, U32, U64,
            F32, L64,
    };

    private int currentLookupId;

    public ValueType lookUpType(final Token type) {
        return Arrays.stream(TYPES)
                .filter(valueType -> valueType.getName().equals(type.asString()))
                .findFirst()
                .orElse(null);
    }

    public Parameter validateRef(final Token name, final Token type) {
        if (type.getType() != TokenType.TYPE)
            throw new IllegalArgumentException("can't lookup type of none 'type' token!");

        ValueType valueType = lookUpType(type);

        Parameter.ReferenceParameter referenceParameter = new Parameter
                .ReferenceParameter(name, valueType, currentLookupId++, 0);

        return referenceParameter;
    }
}
