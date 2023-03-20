package language.vm.library.impl.types;

import language.backend.compiler.bytecode.values.Value;
import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

import java.util.List;
import java.util.Map;

@LibraryClass(className = "types")
public class StdType {

    @LibraryMethod
    public String type(final Object value) {
        return Value.fromObject(value).type();
    }

    @LibraryMethod
    public Map<Value, Value> map(final Object map) {
        return Value.fromObject(map).asMap();
    }

    @LibraryMethod
    public List<Value> list(final Object list) {
        return Value.fromObject(list).asList();
    }

    //
    //        define("isList", (args) -> NativeResult.Ok(new Value(args[0].isList)), Types.BOOL, 1);
    //        define("isFunction", (args) -> NativeResult.Ok(new Value(args[0].isClosure)), Types.BOOL, 1);
    //        define("isBoolean", (args) -> NativeResult.Ok(new Value(args[0].isBool)), Types.BOOL, 1);
    //        define("isDict", (args) -> NativeResult.Ok(new Value(args[0].isMap)), Types.BOOL, 1);
    //        define("isNumber", (args) -> NativeResult.Ok(new Value(args[0].isNumber)), Types.BOOL, 1);
    //        define("isString", (args) -> NativeResult.Ok(new Value(args[0].isString)), Types.BOOL, 1);
    //
    //        define("str", (args) -> NativeResult.Ok(new Value(args[0] != null ? args[0].asString() : "null")), Types.STRING, 1);
    //        define("list", (args) -> NativeResult.Ok(new Value(args[0].asList())), Types.LIST, 1);
    //        define("bool", (args) -> NativeResult.Ok(new Value(args[0].asBool())), Types.BOOL, 1);
    //        define("num", (args) -> NativeResult.Ok(new Value(args[0].asNumber())), Types.FLOAT, 1);
    //        define("chr", (args) -> NativeResult.Ok(new Value(new String(
    //                new byte[] { args[0].asNumber().byteValue() }
    //        ))), Types.STRING, Types.INT);
    //        define("chrs", (args) -> NativeResult.Ok(new Value(new String(
    //                args[0].asBytes()
    //        ))), Types.STRING, Types.BYTES);
    //        define("tuple", (args) -> NativeResult.Ok(new Value(args)),
    //                new ClassObjectType(Types.ANY, new Type[0], new GenericType[0], false) {
    //                    @Override
    //                    public Type call(Type[] arguments, Type[] generics) {
    //                        if (generics.length > 0) {
    //                            return null;
    //                        }
    //                        return new TupleType(arguments);
    //                    }
    //                }
    //        );

}
