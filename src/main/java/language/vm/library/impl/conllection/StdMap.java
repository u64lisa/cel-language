package language.vm.library.impl.conllection;

import language.backend.compiler.bytecode.values.Value;
import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

@LibraryClass(className = "Map")
public class StdMap {

    @LibraryMethod
    public void set(final Object map, final Object key, final Object value) {
        Value.fromObject(map)
                .asMap().put(Value.fromObject(key), Value.fromObject(value));
    }

    @LibraryMethod
    public void delete(final Object map, final Object key) {
        Value.fromObject(map)
                .asMap().remove(Value.fromObject(key));
    }

    //        define("overset", (args) -> {
    //            args[0].asMap().replace(args[1], args[2]);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.MAP, Types.ANY, Types.ANY);
    //        define("get", (args) -> {
    //            if (args[0].asMap().containsKey(args[1]))
    //                return NativeResult.Ok(args[0].get(args[1]));
    //            return NativeResult.Err("Key", "Key not found");
    //        }, Types.ANY, Types.MAP, Types.ANY);
    //        define("delete", (args) -> {
    //            args[0].delete(args[1]);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.MAP, Types.ANY);

}
