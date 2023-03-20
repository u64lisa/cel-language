package language.vm.library.impl.conllection;

import language.backend.compiler.bytecode.values.Value;
import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

import java.util.List;

@LibraryClass(className = "List")
public class StdList {

    @LibraryMethod
    public int size(Object list) {
        final Value value = (Value) list;
        return value.asList().size();
    }

    @LibraryMethod
    public void append(final Object listRaw, final Object element) {
        Value list = Value.fromObject(listRaw);
        Value value = Value.fromObject(element);

        list.add(value);
    }

    //define("contains", args -> {
    //    Value list = args[0];
    //    Value val = args[1];
    //    return NativeResult.Ok(new Value(list.asList().contains(val)));
    //}, Types.BOOL, 2);
    //define("indexOf", args -> {
    //    Value list = args[0];
    //    Value val = args[1];
    //    return NativeResult.Ok(new Value(list.asList().indexOf(val)));
    //}, Types.INT, 2);

    // // List Functions
    //        define("append", (args) -> {
    //            Value list = args[0];
    //            Value value = args[1];
    //
    //            list.append(value);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.LIST, Types.ANY);
    //        define("remove", (args) -> {
    //            Value list = args[0];
    //            Value value = args[1];
    //
    //            list.remove(value);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.LIST, Types.ANY);
    @LibraryMethod
    public Object pop(final Object listRaw, int index) {
        Value list = Value.fromObject(listRaw);

        if (index < 0 || index >= list.asList().size()) {
            throw new IllegalStateException("index out of bounds for list pop");
        }

        return list.pop((double) index);
    }

    //        define("pop", (args) -> {
    //            Value list = args[0];
    //            Value index = args[1];
    //
    //            if (index.asNumber() < 0 || index.asNumber() >= list.asList().size()) {
    //                return NativeResult.Err("Index", "Index out of bounds");
    //            }
    //
    //            return NativeResult.Ok(list.pop(index.asNumber()));
    //        }, Types.ANY, Types.LIST, Types.INT);

    //        define("extend", (args) -> {
    //            Value list = args[0];
    //            Value other = args[1];
    //
    //            list.add(other);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.LIST, Types.LIST);

    //        define("insert", (args) -> {
    //            Value list = args[0];
    //            Value index = args[2];
    //            Value value = args[1];
    //
    //            if (list.asList().size() < index.asNumber() || index.asNumber() < 0) {
    //                return NativeResult.Err("Scope", "Index out of bounds");
    //            }
    //
    //            list.insert(index.asNumber(), value);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.LIST, Types.ANY, Types.INT);

    @LibraryMethod
    public void setIndex(final Object list, final Object element, final int index) {
        Value listVal = Value.fromObject(list);
        Value indexVal = Value.fromObject(index);
        Value elementVal = Value.fromObject(element);

        if (indexVal.asNumber() >= listVal.asList().size()) {
            throw new IllegalStateException("Index out of bounds for list!");
        }

        listVal.set(indexVal.asNumber(), elementVal);
    }

    //        define("setIndex", (args) -> {
    //            Value list = args[0];
    //            Value index = args[2];
    //            Value value = args[1];
    //
    //            if (index.asNumber() >= list.asList().size()) {
    //                return NativeResult.Err("Index", "Index out of bounds");
    //            }
    //
    //            list.set(index.asNumber(), value);
    //            return NativeResult.Ok();
    //        }, Types.VOID, Types.LIST, Types.ANY, Types.INT);

    @LibraryMethod
    public List<Value> sublist(final Object listRaw, int start, int end) {
        Value list = Value.fromObject(listRaw);

        if (list.asList().size() < end || start < 0 || end < start) {
            throw new IllegalStateException("index out of bounds for sublist!");
        }

        return list.asList().subList(start, end);
    }

}
