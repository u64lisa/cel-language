package language.backend.compiler.bytecode.types.primitives.primitive;

import language.backend.compiler.bytecode.types.primitives.numbers.*;
import language.backend.compiler.bytecode.types.primitives.other.BooleanType;
import language.backend.compiler.bytecode.types.primitives.other.BytesType;
import language.backend.compiler.bytecode.types.primitives.other.ResultType;
import language.backend.compiler.bytecode.types.primitives.other.StringType;
import language.backend.compiler.bytecode.types.primitives.collection.MapType;
import language.backend.compiler.bytecode.types.primitives.collection.ListType;

public class PrimitiveTypes {
    public static final I32Type INT = I32Type.INSTANCE;
    public static final F64Type FLOAT = F64Type.INSTANCE;
    public static final L64Type LONG = L64Type.INSTANCE;
    public static final I16Type SHORT = I16Type.INSTANCE;
    public static final I8Type BYTE = I8Type.INSTANCE;
    public static final I64Type DOUBLE = I64Type.INSTANCE;

    public static final BooleanType BOOL = BooleanType.INSTANCE;
    public static final StringType STRING = StringType.INSTANCE;
    public static final BytesType BYTES = BytesType.INSTANCE;

    public static final ListType LIST = ListType.INSTANCE;
    public static final MapType MAP = MapType.INSTANCE;

    public static final ResultType RESULT = ResultType.INSTANCE;
}
