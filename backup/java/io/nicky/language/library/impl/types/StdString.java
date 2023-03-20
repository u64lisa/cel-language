package language.vm.library.impl.types;

import language.backend.compiler.bytecode.values.bytecode.NativeResult;
import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

@LibraryClass(className = "String")
public class StdString {

    // define("split", (args) -> {
    //            String str = args[0].asString();
    //            String delim = args[1].asString();
    //            String[] result = str.split(delim);
    //            List<Value> list = new ArrayList<>();
    //            for (String s : result) {
    //                list.add(new Value(s));
    //            }
    //            return NativeResult.Ok(new Value(list));
    //        }, Types.LIST, Types.STRING, Types.STRING);

    @LibraryMethod
    public String substr(final String str, int start, int end) {
        while (start < 0) start = str.length() + start;
        while (end < 0) end = str.length() + end;

        if (start > str.length()) start = str.length();
        if (end > str.length()) end = str.length();

        return str.substring(start, end);
    }

    //        define("substr", (args) -> {
    //            String str = args[0].asString();
    //            int start = args[1].asNumber().intValue();
    //            int end = args[2].asNumber().intValue();
    //
    //            while (start < 0) start = str.length() + start;
    //            while (end < 0) end = str.length() + end;
    //
    //            if (start > str.length()) start = str.length();
    //            if (end > str.length()) end = str.length();
    //
    //            return NativeResult.Ok(new Value(str.substring(start, end)));
    //        }, Types.STRING, Types.STRING, Types.INT, Types.INT);

    //        define("join", (args) -> {
    //            Value str = args[0];
    //            Value list = args[1];
    //
    //            List<String> strings = new ArrayList<>();
    //            for (Value val : list.asList())
    //                strings.add(val.asString());
    //
    //            return NativeResult.Ok(new Value(String.join(str.asString(), strings)));
    //        }, Types.STRING, Types.STRING, Types.LIST);
    //        define("replace", (args) -> {
    //            String str = args[0].asString();
    //            String old = args[1].asString();
    //            String newStr = args[2].asString();
    //            return NativeResult.Ok(new Value(str.replace(old, newStr)));
    //        }, Types.STRING, Types.STRING, Types.STRING, Types.STRING);
    //        define("strUpper",
    //                (args) -> NativeResult.Ok(new Value(args[0].asString().toUpperCase())),
    //                Types.STRING, Types.STRING);
    //        define("strLower",
    //                (args) -> NativeResult.Ok(new Value(args[0].asString().toLowerCase())),
    //                Types.STRING, Types.STRING);
    //        define("strShift", (args) -> {
    //            String str = args[0].asString();
    //            StringBuilder sb = new StringBuilder();
    //            for (char c : str.toCharArray()) {
    //                String s = Character.toString(c);
    //                sb.append(SHIFT.getOrDefault(s, s));
    //            }
    //            return NativeResult.Ok(new Value(sb.toString()));
    //        }, Types.STRING, Types.STRING);
    //        define("strUnshift", (args) -> {
    //            String str = args[0].asString();
    //            StringBuilder sb = new StringBuilder();
    //            for (char c : str.toCharArray()) {
    //                String s = Character.toString(c);
    //                sb.append(UN_SHIFT.getOrDefault(s, s));
    //            }
    //            return NativeResult.Ok(new Value(sb.toString()));
    //        }, Types.STRING, Types.STRING);

}
