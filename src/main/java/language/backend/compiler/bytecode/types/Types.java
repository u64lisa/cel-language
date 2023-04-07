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
    public static final I32Type INT = PrimitiveTypes.INT;
    public static final I128Type LONG = PrimitiveTypes.LONG;
    public static final I16Type SHORT = PrimitiveTypes.SHORT;
    public static final F32Type FLOAT = PrimitiveTypes.FLOAT;
    public static final I8Type BYTE = PrimitiveTypes.BYTE;
    public static final I64Type DOUBLE = PrimitiveTypes.DOUBLE;
    public static final BooleanType BOOL = PrimitiveTypes.BOOL;
    public static final ListType LIST = PrimitiveTypes.LIST;
    public static final MapType MAP = PrimitiveTypes.MAP;
    public static final StringType STRING = PrimitiveTypes.STRING;
    public static final BytesType BYTES = PrimitiveTypes.BYTES;
    public static final ResultType RESULT = PrimitiveTypes.RESULT;
    public static final SpreadType SPREAD = SpreadType.INSTANCE;
}
