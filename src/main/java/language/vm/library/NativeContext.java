package language.vm.library;

import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import language.backend.compiler.bytecode.types.GenericType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.objects.ClassObjectType;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.bytecode.Native;
import language.backend.compiler.bytecode.values.bytecode.NativeResult;
import language.vm.VirtualMachine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class NativeContext {

    public static final Map<String, Type> GLOBAL_TYPES = new HashMap<>();

    public static final Logger SYSTEM_LOGGER = ImplLogger.getInstance();
    
    static final HashMap<String, String> SHIFT = new HashMap<>() {{
        put("1", "!");
        put("2", "@");
        put("3", "#");
        put("4", "$");
        put("5", "%");
        put("6", "^");
        put("7", "&");
        put("8", "*");
        put("9", "(");
        put("0", ")");

        put("`", "~");

        put("'", "\"");
        put(";", ":");

        put("/", "?");
        put(".", ">");
        put(",", "<");

        put("[", "{");
        put("]", "}");
        put("\\", "|");

        put("-", "_");
        put("=", "+");
    }};


    static final HashMap<String, String> UN_SHIFT = new HashMap<>() {{
        forEach((key, value) -> put(value, key));
    }};

    Map<String, Type> fields = new HashMap<>();
    
    private final VirtualMachine virtualMachine;

    public NativeContext(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;

        this.initNativeSTD();
    }
    
    public void initNativeSTD() {
        func("writeFile", (args) -> {
            String path = dir(args[0]);
            String data = args[1].asString();
            File file = new File(path);
            try {
                boolean created = file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write(data);
                writer.close();
                return Ok(created);
            } catch (IOException e) {
                return Err("Internal", "Could not write file (" + e.getMessage() + ")");
            }
        }, Types.BOOL, Types.STRING, Types.ANY);

        // File Creation
        func("fileExists", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            return Ok(file.exists());
        }, Types.BOOL, Types.STRING);
        func("makeDirs", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            return Ok(file.mkdirs());
        }, Types.BOOL, Types.STRING);

        // Directories
        func("listDirContents", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists() || !file.isDirectory())
                return Err("Imaginary Path", "Path not found");

            String[] files = file.list();
            List<String> list = new ArrayList<>(Arrays.asList(files));
            return Ok(list);
        }, Types.LIST, Types.STRING);
        func("isDirectory", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            return Ok(file.isDirectory());
        }, Types.BOOL, Types.STRING);

        // Working Directory
        func("getCWD", (args) -> Ok(System.getProperty("execution_compile_directory")), Types.STRING);
        func("setCWD", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary Path", "Path not found");
            if (!file.isDirectory())
                return Err("Imaginary Path", "Path is not a directory");
            System.setProperty("execution_compile_directory", path);
            return Ok;
        }, Types.VOID, Types.STRING);

        // Serialization
        func("readSerial", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary File", "File not found");

            Value out;
            try {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Object obj = ois.readObject();
                if (obj instanceof Value) {
                    out = (Value) obj;
                }
                else {
                    out = Value.fromObject(obj);
                }

                ois.close();
                fis.close();
            } catch (IOException | ClassNotFoundException e) {
                return Err("Internal", "Could not load file (" + e.getMessage() + ")");
            }

            return Ok(out);
        }, Types.ANY, Types.STRING);
        func("readBytes", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary File", "File not found");

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(path));
            } catch (IOException e) {
                return Err("Internal", "Could not load file (" + e.getMessage() + ")");
            }

            return Ok(bytes);
        }, Types.BYTES, Types.STRING);
        func("writeSerial", (args) -> {
            String path = dir(args[0]);
            Value obj = args[1];
            File file = new File(path);
            try {
                boolean created = file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                if (obj.isBytes) {
                    fos.write(obj.asBytes());
                }
                else {
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(obj.asObject());
                    oos.close();
                }
                fos.close();
                return Ok(created);
            } catch (IOException e) {
                return Err("Internal", "Could not write file (" + e.getMessage() + ")");
            }
        }, Types.BOOL, Types.STRING, Types.ANY);
        func("epoch", (args) -> Ok(System.currentTimeMillis()), Types.INT);

        func("halt", (args) -> {
            try {
                Thread.sleep(args[0].asNumber().intValue());
            } catch (InterruptedException e) {
                return Err("Internal", "Interrupted");
            }
            return Ok;
        }, Types.VOID, Types.INT);

        func("stopwatch", (args) -> {
            long start = System.currentTimeMillis();
            NativeResult ret = VirtualMachine.run(args[0].asClosure(), new Value[0]);
            if (!ret.ok()) return ret;
            long end = System.currentTimeMillis();
            return Ok(end - start);
        }, Types.INT, new ClassObjectType(Types.VOID, new Type[0], new GenericType[0], false) );
        // System Library
        // Quick Environment Variables
        func("os", (args) -> Ok(System.getProperty("os.name")), Types.STRING);
        func("home", (args) -> Ok(System.getProperty("user.home")), Types.STRING);

        // Execution
        func("execute", (args) -> {
            String cmd = args[0].asString();
            Runtime rt = Runtime.getRuntime();
            try {
                Process pr = rt.exec(cmd);
                return processOut(pr);
            } catch (Exception e) {
                return Err("Internal", e.getMessage());
            }
        }, Types.STRING, Types.STRING);
        func("executeFloor", (args) -> {
            List<Value> args2 = args[0].asList();
            String[] cmd = new String[args2.size()];
            for (int i = 0; i < args2.size(); i++)
                cmd[i] = args[i].asString();

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            try {
                Process pr = pb.start();
                return processOut(pr);
            } catch (Exception e) {
                return Err("Internal", e.getMessage());
            }
        }, Types.STRING, Types.LIST);

        // Environment Variables
        func("envVarExists", (args) -> Ok(System.getenv(args[0].asString()) != null), Types.BOOL, Types.STRING);
        func("getEnvVar", (args) -> {
            String name = args[0].asString();
            String value = System.getenv(name);
            if (value == null)
                return Err("Scope", "Environment variable '" + name + "' does not exist");
            return Ok(value);
        }, Types.STRING, Types.STRING);
        func("setEnvVar", (args) -> {
            String name = args[0].asString();
            String value = args[1].asString();
            System.setProperty(name, value);
            return Ok;
        }, Types.VOID, Types.STRING, Types.STRING);

        // System Properties
        func("propExists", (args) -> Ok(System.getProperty(args[0].asString()) != null), Types.BOOL, Types.STRING);
        func("getProp", (args) -> {
            String name = args[0].asString();
            String value = System.getProperty(name);
            if (value == null)
                return Err("Scope", "System property '" + name + "' does not exist");
            return Ok(value);
        }, Types.STRING, Types.STRING);
        func("setProp", (args) -> {
            String name = args[0].asString();
            String value = args[1].asString();
            System.setProperty(name, value);
            return Ok;
        }, Types.VOID, Types.STRING, Types.STRING);

        // System
        func("exit", (args) -> {
            int code = args[0].asNumber().intValue();
            System.exit(code);
            return Ok;
        }, Types.VOID, Types.INT);
        func("range", (args) -> {
            double start = args[0].asNumber();
            double end = args[1].asNumber();
            double step = args[2].asNumber();
            List<Double> list = new ArrayList<>();
            for (double i = start; i < end; i += step)
                list.add(i);
            return Ok(list);
        }, Types.LIST, Types.FLOAT, Types.FLOAT, Types.FLOAT);
        func("linear", (args) -> {
            double start = args[0].asNumber();
            double end = args[1].asNumber();
            double step = args[2].asNumber();

            double m = args[3].asNumber();
            double b = args[4].asNumber();

            List<Double> list = new ArrayList<>();
            for (double i = start; i < end; i += step)
                list.add(m * i + b);
            return Ok(list);
        }, Types.LIST, Types.FLOAT, Types.FLOAT, Types.FLOAT, Types.FLOAT, Types.FLOAT);
        func("quadratic", (args) -> {
            double start = args[0].asNumber();
            double end = args[1].asNumber();
            double step = args[2].asNumber();

            double a = args[3].asNumber();
            double b = args[4].asNumber();
            double c = args[5].asNumber();

            List<Double> list = new ArrayList<>();
            for (double i = start; i < end; i += step)
                list.add(a * i * i + b * i + c);
            return Ok(list);
        }, Types.LIST, Types.FLOAT, Types.FLOAT, Types.FLOAT, Types.FLOAT, Types.FLOAT, Types.FLOAT);
        define("asm", stack -> {

            System.out.println(">> " + stack[0]);

            return NativeResult.Ok(null);

        }, Types.RESULT, Types.ANY);

        // IO Function

        define("printback", (args) -> {
            SYSTEM_LOGGER.out(args[0]);
            return NativeResult.Ok(args[0]);
        }, Types.ANY, 1);

        define("field", (args) -> {
            SYSTEM_LOGGER.out(args[0].asString());
            return NativeResult.Ok(new Value(SYSTEM_LOGGER.readLine()));
        }, Types.STRING, 1);
        define("nfield", (args) -> {
            Pattern p = Pattern.compile("-?\\d+(\\.\\d+)?");
            SYSTEM_LOGGER.out(args[0].asString());
            String text;
            do {
                text = SYSTEM_LOGGER.readLine();
            } while (!p.matcher(text).matches());
            return NativeResult.Ok(new Value(Double.parseDouble(text)));
        }, Types.FLOAT, 1);

        define("clear", (args) -> {
            try {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                }
                else {
                    Runtime.getRuntime().exec("clear");
                }
            } catch (IOException | InterruptedException ignored) {}
            return NativeResult.Ok();
        }, Types.VOID, 0);

        // Number Functions

        // Convert number list to byte[]
         define("byter", (args) -> {
            List<Value> list = args[0].asList();
            byte[] bytes = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Value v = list.get(i);
                if (!v.isNumber || v.asNumber().byteValue() != v.asNumber())
                    return NativeResult.Err("Type", "List must contain only bytes");
                bytes[i] = v.asNumber().byteValue();
            }
            return NativeResult.Ok(new Value(bytes));
        }, Types.LIST, Types.LIST);

        define("floating",
                (args) -> NativeResult.Ok(new Value(Math.round(args[0].asNumber()) != args[0].asNumber())),
                Types.BOOL, Types.FLOAT);

        // String Functions
        define("split", (args) -> {
            String str = args[0].asString();
            String delim = args[1].asString();
            String[] result = str.split(delim);
            List<Value> list = new ArrayList<>();
            for (String s : result) {
                list.add(new Value(s));
            }
            return NativeResult.Ok(new Value(list));
        }, Types.LIST, Types.STRING, Types.STRING);
        define("substr", (args) -> {
            String str = args[0].asString();
            int start = args[1].asNumber().intValue();
            int end = args[2].asNumber().intValue();

            while (start < 0) start = str.length() + start;
            while (end < 0) end = str.length() + end;

            if (start > str.length()) start = str.length();
            if (end > str.length()) end = str.length();

            return NativeResult.Ok(new Value(str.substring(start, end)));
        }, Types.STRING, Types.STRING, Types.INT, Types.INT);
        define("join", (args) -> {
            Value str = args[0];
            Value list = args[1];

            List<String> strings = new ArrayList<>();
            for (Value val : list.asList())
                strings.add(val.asString());

            return NativeResult.Ok(new Value(String.join(str.asString(), strings)));
        }, Types.STRING, Types.STRING, Types.LIST);
        define("replace", (args) -> {
            String str = args[0].asString();
            String old = args[1].asString();
            String newStr = args[2].asString();
            return NativeResult.Ok(new Value(str.replace(old, newStr)));
        }, Types.STRING, Types.STRING, Types.STRING, Types.STRING);
        define("strUpper",
                (args) -> NativeResult.Ok(new Value(args[0].asString().toUpperCase())),
                Types.STRING, Types.STRING);
        define("strLower",
                (args) -> NativeResult.Ok(new Value(args[0].asString().toLowerCase())),
                Types.STRING, Types.STRING);
        define("strShift", (args) -> {
            String str = args[0].asString();
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                String s = Character.toString(c);
                sb.append(SHIFT.getOrDefault(s, s));
            }
            return NativeResult.Ok(new Value(sb.toString()));
        }, Types.STRING, Types.STRING);
        define("strUnshift", (args) -> {
            String str = args[0].asString();
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                String s = Character.toString(c);
                sb.append(UN_SHIFT.getOrDefault(s, s));
            }
            return NativeResult.Ok(new Value(sb.toString()));
        }, Types.STRING, Types.STRING);

        // List Functions
        define("append", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.append(value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY);
        define("remove", (args) -> {
            Value list = args[0];
            Value value = args[1];

            list.remove(value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY);
        define("pop", (args) -> {
            Value list = args[0];
            Value index = args[1];

            if (index.asNumber() < 0 || index.asNumber() >= list.asList().size()) {
                return NativeResult.Err("Index", "Index out of bounds");
            }

            return NativeResult.Ok(list.pop(index.asNumber()));
        }, Types.ANY, Types.LIST, Types.INT);
        define("extend", (args) -> {
            Value list = args[0];
            Value other = args[1];

            list.add(other);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.LIST);
        define("insert", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            if (list.asList().size() < index.asNumber() || index.asNumber() < 0) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            list.insert(index.asNumber(), value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY, Types.INT);
        define("setIndex", (args) -> {
            Value list = args[0];
            Value index = args[2];
            Value value = args[1];

            if (index.asNumber() >= list.asList().size()) {
                return NativeResult.Err("Index", "Index out of bounds");
            }

            list.set(index.asNumber(), value);
            return NativeResult.Ok();
        }, Types.VOID, Types.LIST, Types.ANY, Types.INT);
        define("sublist", (args) -> {
            Value list = args[0];
            Value start = args[1];
            Value end = args[2];

            if (list.asList().size() < end.asNumber() || start.asNumber() < 0 || end.asNumber() < start.asNumber()) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            return NativeResult.Ok(new Value(list.asList().subList(start.asNumber().intValue(),
                    end.asNumber().intValue())));
        }, Types.LIST, Types.LIST, Types.INT, Types.INT);

        // Collection Functions
        define("size", args -> {
            Value list = args[0];
            return NativeResult.Ok(new Value(list.asList().size()));
        }, Types.INT, 1);
        define("contains", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().contains(val)));
        }, Types.BOOL, 2);
        define("indexOf", args -> {
            Value list = args[0];
            Value val = args[1];
            return NativeResult.Ok(new Value(list.asList().indexOf(val)));
        }, Types.INT, 2);

        define("epoch", (args) -> NativeResult.Ok(Value.fromObject(System.currentTimeMillis())), Types.FLOAT);

        // Results
        define("ok", args -> NativeResult.Ok(new Value(args[0].asBool())), Types.BOOL, Types.RESULT);
        define("resolve", args -> {
            if (!args[0].asBool())
                return NativeResult.Err("Unresolved", "Unresolved error in catcher");
            return NativeResult.Ok(args[0].asRes().getValue());
        }, Types.ANY, Types.RESULT);
        define("catch", args -> {
            if (!args[0].asBool())
                return NativeResult.Ok(new Value(args[0].asList()));
            return NativeResult.Ok();
        }, Types.ANY, Types.RESULT);
        define("fail", args -> {
            if (args[0].asBool())
                return NativeResult.Ok();
            return NativeResult.Err("Released", args[0].toString());
        }, Types.VOID, Types.RESULT);

    }

    public void define(String name, Native.Method method, Type returnType, int argc) {
        if (virtualMachine == null) {
            Type[] types = new Type[argc];
            for (int i = 0; i < argc; i++)
                types[i] = Types.ANY;
            GLOBAL_TYPES.put(name, new ClassObjectType(returnType, types, new GenericType[0], false));
        } else
            virtualMachine.defineNative(name, method, argc);
    }

    public void define(String name, Native.Method method, Type returnType, Type... types) {
        if (virtualMachine == null)
            GLOBAL_TYPES.put(name, new ClassObjectType(returnType, types, new GenericType[0], false));
        else
            virtualMachine.defineNative(name, method, types);
    }

    // tuple(1, 2, 3) -> (int, int, int), tuple(1, 2, "3") -> (int, int, String)
    public void define(String name, Native.Method method, ClassObjectType type) {
        if (virtualMachine == null)
            GLOBAL_TYPES.put(name, type);
        else
            virtualMachine.defineNative(name, method, type.varargs ? -1 : type.parameterTypes.length);
    }

    protected static final NativeResult Ok = NativeResult.Ok();

    protected static NativeResult Ok(Value val) {
        return NativeResult.Ok(val);
    }

    protected static NativeResult Ok(Object obj) {
        return Ok(Value.fromObject(obj));
    }

    protected static NativeResult Err(String title, String msg) {
        return NativeResult.Err(title, msg);
    }

    protected void func(String name, Native.Method method, Type returnType, int argc) {
        if (virtualMachine == null) {
            Type[] types = new Type[argc];
            for (int i = 0; i < argc; i++)
                types[i] = Types.ANY;
            fields.put(name, new ClassObjectType(returnType, types, new GenericType[0], false));
        } else
            virtualMachine.defineNative("native-std", name, method, argc);
    }

    protected void func(String name, Native.Method method, Type returnType, Type... types) {
        if (virtualMachine == null)
            fields.put(name, new ClassObjectType(returnType, types, new GenericType[0], false));
        else
            virtualMachine.defineNative("native-std", name, method, types);
    }

    private NativeResult processOut(Process pr) throws InterruptedException, IOException {
        pr.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line).append("\n");
        return Ok(sb.toString());
    }


    private String dir(Value val) {
        String dir = val.asString();
        //noinspection RegExpRedundantEscape
        if (!dir.matches("^([A-Z]:|\\.|\\/|\\\\).*"))
            dir = System.getProperty("^^") + "/" + dir;
        return dir;
    }

    protected void var(String name, Value val, Type type) {
        if (virtualMachine == null)
            fields.put(name, type);
        else
            virtualMachine.defineVar("native-std", name, val);
    }

    protected void var(String name, Object val, Type type) {
        var(name, Value.fromObject(val), type);
    }


    public static void init(VirtualMachine virtualMachine) {
        new NativeContext(virtualMachine);
    }

}
