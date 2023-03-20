package language.backend.compiler.bytecode.types;

import language.backend.compiler.bytecode.types.primitives.collection.MapType;
import language.backend.compiler.bytecode.types.primitives.collection.ListType;
import language.backend.compiler.bytecode.types.primitives.numbers.*;
import language.backend.compiler.bytecode.types.primitives.other.BooleanType;
import language.backend.compiler.bytecode.types.primitives.other.BytesType;
import language.backend.compiler.bytecode.types.primitives.other.ResultType;
import language.backend.compiler.bytecode.types.primitives.other.StringType;
import language.backend.compiler.bytecode.types.primitives.primitive.PrimitiveTypes;


public class Types {
    public static final VoidType VOID = VoidType.INSTANCE;
    public static final AnyType ANY = AnyType.INSTANCE;
    public static final IntType INT = PrimitiveTypes.INT;
    public static final LongType LONG = PrimitiveTypes.LONG;
    public static final ShortType SHORT = PrimitiveTypes.SHORT;
    public static final FloatType FLOAT = PrimitiveTypes.FLOAT;
    public static final ByteType BYTE = PrimitiveTypes.BYTE;
    public static final DoubleType DOUBLE = PrimitiveTypes.DOUBLE;
    public static final BooleanType BOOL = PrimitiveTypes.BOOL;
    public static final ListType LIST = PrimitiveTypes.LIST;
    public static final MapType MAP = PrimitiveTypes.MAP;
    public static final StringType STRING = PrimitiveTypes.STRING;
    public static final BytesType BYTES = PrimitiveTypes.BYTES;
    public static final ResultType RESULT = PrimitiveTypes.RESULT;
    public static final SpreadType SPREAD = SpreadType.INSTANCE;
}
