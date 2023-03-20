package language.backend.compiler.bytecode.types.primitives.primitive;

import language.backend.compiler.bytecode.types.primitives.numbers.*;
import language.backend.compiler.bytecode.types.primitives.other.BooleanType;
import language.backend.compiler.bytecode.types.primitives.other.BytesType;
import language.backend.compiler.bytecode.types.primitives.other.ResultType;
import language.backend.compiler.bytecode.types.primitives.other.StringType;
import language.backend.compiler.bytecode.types.primitives.collection.MapType;
import language.backend.compiler.bytecode.types.primitives.collection.ListType;

public class PrimitiveTypes {
    public static final IntType INT = IntType.INSTANCE;
    public static final FloatType FLOAT = FloatType.INSTANCE;
    public static final LongType LONG = LongType.INSTANCE;
    public static final ShortType SHORT = ShortType.INSTANCE;
    public static final ByteType BYTE = ByteType.INSTANCE;
    public static final DoubleType DOUBLE = DoubleType.INSTANCE;

    public static final BooleanType BOOL = BooleanType.INSTANCE;
    public static final StringType STRING = StringType.INSTANCE;
    public static final BytesType BYTES = BytesType.INSTANCE;

    public static final ListType LIST = ListType.INSTANCE;
    public static final MapType MAP = MapType.INSTANCE;

    public static final ResultType RESULT = ResultType.INSTANCE;
}
