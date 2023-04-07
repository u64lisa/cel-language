package language.backend.compiler.bytecode.values;

import language.backend.compiler.bytecode.ChunkCode;
import language.backend.compiler.bytecode.values.classes.BoundMethod;
import language.backend.compiler.bytecode.values.classes.Instance;
import language.backend.compiler.bytecode.values.classes.Namespace;
import language.backend.compiler.bytecode.values.classes.LanguageClass;
import language.backend.compiler.bytecode.values.enums.LanguageEnum;
import language.backend.compiler.bytecode.values.enums.LanguageEnumChild;
import language.backend.compiler.bytecode.values.bytecode.*;
import language.vm.VirtualMachineResult;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Value {
    protected double number;
    protected String string;
    protected boolean bool;
    protected List<Value> list;
    protected Map<Value, Value> map;
    protected ByteCode func;
    protected Native nativeFunc;
    protected Var var;
    protected Closure closure;
    protected LanguageClass languageClass;
    protected Instance instance;
    protected BoundMethod boundMethod;
    protected Namespace namespace;
    protected LanguageEnum enumParent;
    protected LanguageEnumChild enumChild;
    protected Spread spread;
    protected Value ref;
    protected byte[] bytes;
    protected Result res;
    protected String patternBinding;
    protected Pattern pattern;
    protected Value[] tuple;

    public boolean isNull = false;
    public boolean isNumber = false;
    public boolean isString = false;
    public boolean isList = false;
    public boolean isMap = false;
    public boolean isBool = false;
    public boolean isFunc = false;
    public boolean isNativeFunc = false;
    public boolean isVar = false;
    public boolean isClosure = false;
    public boolean isClass = false;
    public boolean isInstance = false;
    public boolean isBoundMethod = false;
    public boolean isNamespace = false;
    public boolean isEnumParent = false;
    public boolean isEnumChild = false;
    public boolean isSpread = false;
    public boolean isRef = false;
    public boolean isBytes = false;
    public boolean isRes = false;
    public boolean isPatternBinding = false;
    public boolean isPattern = false;
    public boolean isTuple = false;

    public Value() {
        this.isNull = true;
    }

    public Value(Value... values) {
        this.tuple = values;
        this.isTuple = true;
    }

    public Value(Pattern pattern) {
        this.pattern = pattern;
        this.isPattern = true;
    }

    public Value(Result res) {
        this.res = res;
        this.isRes = true;
    }

    public Value(byte[] bytes) {
        this.bytes = bytes;
        this.isBytes = true;
    }

    public Value(Value value) {
        this.ref = value;
        this.isRef = true;
    }

    public static Value patternBinding(String patternBinding) {
        Value value = new Value();
        value.patternBinding = patternBinding;
        value.isPatternBinding = true;
        value.isNull = false;
        return value;
    }

    public Value(Spread spread) {
        this.spread = spread;
        this.isSpread = true;
    }

    public Value(LanguageEnum enumParent) {
        this.enumParent = enumParent;
        this.isEnumParent = true;
    }

    public Value(LanguageEnumChild enumChild) {
        this.enumChild = enumChild;
        this.isEnumChild = true;
    }

    public Value(Namespace namespace) {
        this.namespace = namespace;
        this.isNamespace = true;
    }

    public Value(BoundMethod boundMethod) {
        this.boundMethod = boundMethod;
        this.isBoundMethod = true;
    }

    public Value(Closure closure) {
        this.closure = closure;
        this.isClosure = true;
    }

    public Value(LanguageClass languageClass) {
        this.languageClass = languageClass;
        this.isClass = true;
    }

    public Value(Instance instance) {
        this.instance = instance;
        this.isInstance = true;
    }

    public Value(Var var) {
        this.var = var;
        this.isVar = true;
    }

    public Value(double number) {
        this.number = number;
        this.isNumber = true;
    }

    public Value(String string) {
        this.string = string;
        this.isString = true;
    }

    public Value(boolean bool) {
        this.bool = bool;
        this.isBool = true;
    }

    public Value(List<Value> list) {
        this.list = list;
        this.isList = true;
    }

    public Value(Map<Value, Value> map) {
        this.map = map;
        this.isMap = true;
    }

    public Value(ByteCode func) {
        this.func = func;
        this.isFunc = true;
    }

    public Value(Native nativeFunc) {
        this.nativeFunc = nativeFunc;
        this.isNativeFunc = true;
    }

    public Double asNumber() {
        if (isNumber) {
            return number;
        } else if (isString) {
            return (double) string.length();
        } else if (isBool) {
            return bool ? 1.0 : 0.0;
        } else if (isList) {
            return (double) list.size();
        } else if (isMap) {
            return (double) map.size();
        } else if (isInstance) {
            return instance.asNumber();
        } else if (isRef) {
            return ref.asNumber();
        } else if (isRes) {
            return res.isError() ? 0.0 : 1.0;
        }
        return 0.0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean asBool() {
        if (isBool) {
            return bool;
        } else if (this.isNull) {
            return false;
        } else if (isNumber) {
            return number != 0.0;
        } else if (isString) {
            return !string.isEmpty();
        } else if (isList) {
            return !list.isEmpty();
        } else if (isMap) {
            return !map.isEmpty();
        } else if (isInstance) {
            return instance.asBool();
        } else if (isRef) {
            return ref.asBool();
        } else if (isRes) {
            return !res.isError();
        }
        return false;
    }

    @SuppressWarnings("DuplicatedCode")
    public String asString() {
        if (isString) {
            return string;
        } else if (this.isNull) {
            return "";
        } else if (isNumber) {
            if (number == Double.MAX_VALUE) {
                return "Infinity";
            } else if (number == Double.MIN_VALUE) {
                return "-Infinity";
            }

            if (Math.floor(number) == number && number < Long.MAX_VALUE && number > Long.MIN_VALUE) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        } else if (isBool) {
            return String.valueOf(bool);
        } else if (isList) {
            StringBuilder result = new StringBuilder("[");
            list.forEach(k -> {
                if (k.isString) {
                    result.append('"').append(k.string).append('"');
                } else {
                    result.append(k.asString());
                }
                result.append(", ");
            });
            if (result.length() > 1) {
                result.setLength(result.length() - 2);
            }
            result.append("]");
            return result.toString();
        } else if (isMap) {
            StringBuilder result = new StringBuilder("{");
            map.forEach((k, v) -> {
                if (k.isString) {
                    result.append('"').append(k.string).append('"');
                } else {
                    result.append(k.asString());
                }
                result.append(": ");
                if (v.isString) {
                    result.append('"').append(v.string).append('"');
                } else {
                    result.append(v.asString());
                }
                result.append(", ");
            });
            if (result.length() > 1) {
                result.setLength(result.length() - 2);
            }
            result.append("}");
            return result.toString();
        } else if (isFunc) {
            return func.toString();
        } else if (isClosure) {
            return closure.byteCode.toString();
        } else if (isVar) {
            return var.toString();
        } else if (isNativeFunc) {
            return nativeFunc.toString();
        } else if (isClass) {
            return languageClass.toString();
        } else if (isInstance) {
            return instance.toString();
        } else if (isBoundMethod) {
            return boundMethod.toString();
        } else if (isNamespace) {
            return namespace.name();
        } else if (isEnumParent) {
            return enumParent.name();
        } else if (isEnumChild) {
            return enumChild.getParent().children().entrySet().stream()
                    .filter(enumElement -> enumElement.getValue().getValue() == enumChild.getValue())
                    .findFirst().map(Map.Entry::getKey).orElse(enumChild.toString());
        } else if (isRef) {
            return ref.asString();
        } else if (isBytes) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++)
                sb.append(bytes[i]).append(", ");

            return "{ " + sb + "len=" + bytes.length + " }";
        } else if (isRes) {
            if (res.isError()) {
                return String.format("(\"%s\" : \"%s\")", res.getErrorMessage(), res.getErrorReason());
            } else {
                return String.format("(%s)", res.getValue());
            }
        } else if (isPatternBinding) {
            return "{ pattern: " + patternBinding + " }";
        } else if (isPattern) {
            StringBuilder sb = new StringBuilder(pattern.value.toString() + " { ");
            for (Map.Entry<String, Value> entry : pattern.cases.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue().asString()).append(", ");
            }
            for (Map.Entry<String, String> entry : pattern.matches.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
            }
            return sb + "}";
        }
        return "";
    }

    public String toString() {
        return asString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Value))
            return false;

        Value o = (Value) obj;
        if (this.isNull)
            return o.isNull;

        else if (this.isNumber)
            return o.isNumber && this.number == o.number;

        else if (this.isString)
            return o.isString && this.string.equals(o.string);

        else if (this.isBool)
            return o.isBool && this.bool == o.bool;

        else if (this.isList)
            return o.isList && this.list.equals(o.list);

        else if (this.isMap)
            return o.isMap && this.map.equals(o.map);

        else if (this.isEnumChild)
            return o.isEnumChild && this.enumChild.equals(o.enumChild);

        return this == o;
    }

    public Value[] asTuple() {
        if (isTuple) {
            return tuple;
        } else if (isList) {
            Value[] tuple = new Value[list.size()];
            for (int i = 0; i < tuple.length; i++) {
                tuple[i] = list.get(i);
            }
            return tuple;
        } else if (isMap) {
            Value[] tuple = new Value[map.size()];
            int i = 0;
            for (Map.Entry<Value, Value> entry : map.entrySet()) {
                tuple[i++] = new Value(entry.getKey(), entry.getValue());
            }
            return tuple;
        }
        return null;
    }

    public List<Value> asList() {
        if (isList) {
            return list;
        } else if (isTuple) {
            return Arrays.asList(tuple);
        } else if (isMap) {
            return new ArrayList<>(map.keySet());
        } else if (isString) {
            String[] lis = string.split("");
            List<Value> list = new ArrayList<>();
            for (String s : lis) {
                list.add(new Value(s));
            }
            return list;
        } else if (this.isNull) {
            return new ArrayList<>();
        } else if (isInstance) {
            return instance.asList();
        } else if (isRef) {
            return ref.asList();
        } else if (isRes) {
            if (res.isError()) {
                return Arrays.asList(new Value(res.getErrorMessage()), new Value(res.getErrorReason()));
            } else {
                return Collections.singletonList(new Value(res.getValue()));
            }
        } else if (isBytes) {
            List<Value> list = new ArrayList<>();
            for (int i = 0; i < bytes.length; i++) {
                list.add(new Value(bytes[i]));
            }
            return list;
        }
        return new ArrayList<>(Collections.singletonList(this));
    }

    public Var asVar() {
        if (isRef) {
            return ref.asVar();
        }
        return var;
    }

    public Map<Value, Value> asMap() {
        if (isMap) {
            return map;
        } else if (this.isNull) {
            return new HashMap<>();
        } else if (isInstance) {
            return instance.asMap();
        } else if (isRef) {
            return ref.asMap();
        } else if (isRes) {
            Map<Value, Value> map = new HashMap<>();
            map.put(new Value("sucess"), new Value(res.getValue()));
            List<Value> key;
            if (res.isError()) {
                key = new ArrayList<>(Arrays.asList(new Value(res.getErrorMessage()), new Value(res.getErrorReason())));
            } else {
                key = new ArrayList<>();
            }
            map.put(new Value("error"), new Value(key));
            return map;
        }
        return new HashMap<>(Collections.singletonMap(this, this));
    }

    public ByteCode asFunc() {
        if (isFunc) {
            return func;
        } else if (this.isClosure) {
            return closure.byteCode;
        } else if (isRef) {
            return ref.asFunc();
        }
        return null;
    }

    public Closure asClosure() {
        if (isClosure) {
            return closure;
        } else if (isRef) {
            return ref.asClosure();
        }
        return null;
    }

    public LanguageClass asClass() {
        if (isClass) {
            return languageClass;
        } else if (isRef) {
            return ref.asClass();
        }
        return null;
    }

    public Native asNative() {
        return nativeFunc;
    }

    public BoundMethod asBoundMethod() {
        if (isBoundMethod) {
            return boundMethod;
        } else if (isRef) {
            return ref.asBoundMethod();
        }
        return null;
    }


    // Mutative Addition
    public VirtualMachineResult add(Value other) {
        if (isNumber) {
            number += other.asNumber();
            return VirtualMachineResult.OK;
        } else if (isList) {
            list.addAll(other.asList());
            return VirtualMachineResult.OK;
        }

        return VirtualMachineResult.ERROR;
    }

    // List Mutators
    public void append(Value value) {
        list.add(value);
    }

    public Value pop(Double index) {
        int i = index.intValue();
        Value value = list.get(i);
        list.remove(i);
        return value;
    }

    public void insert(Double index, Value value) {
        list.add(index.intValue(), value);
    }

    public void set(Double index, Value value) {
        list.set(index.intValue(), value);
    }

    public void remove(Value value) {
        list.remove(value);
    }

    // Map Mutators
    public void set(Value key, Value value) {
        map.put(key, value);
    }

    public void delete(Value key) {
        map.remove(key);
    }

    public Instance asInstance() {
        if (isRef) {
            return ref.asInstance();
        }
        return instance;
    }

    public Result asRes() {
        return res;
    }

    public Namespace asNamespace() {
        if (isRef) {
            return ref.asNamespace();
        }
        return namespace;
    }

    public String toSafeString() {
        if (isInstance) {
            return instance.clazz.name;
        } else if (isVar) {
            return var.toSafeString();
        }
        return toString();
    }

    public Value copy() {
        if (isNumber) {
            return new Value(number);
        } else if (isString) {
            return new Value(string);
        } else if (isBool) {
            return new Value(bool);
        } else if (isList) {
            List<Value> list = new ArrayList<>();
            for (Value value : this.list) {
                list.add(value.copy());
            }
            return new Value(list);
        } else if (isMap) {
            Map<Value, Value> map = new HashMap<>();
            for (Map.Entry<Value, Value> entry : this.map.entrySet()) {
                map.put(entry.getKey().copy(), entry.getValue().copy());
            }
            return new Value(map);
        } else if (isClass) {
            return new Value(languageClass.copy());
        } else if (isInstance) {
            return new Value(instance.copy());
        }
        return this;
    }

    public LanguageEnumChild asEnumChild() {
        if (isRef) {
            return ref.asEnumChild();
        }
        return enumChild;
    }

    public LanguageEnum asEnum() {
        if (isRef) return ref.asEnum();
        return enumParent;
    }

    public Spread asSpread() {
        if (isRef) return ref.asSpread();
        return spread;
    }

    public Value asRef() {
        return ref;
    }

    public Value setRef(Value value) {
        ref = value;
        return this;
    }

    public byte[] asBytes() {
        if (isInstance)
            return instance.asBytes();
        else if (isBytes)
            return bytes;
        return objToBytes(asObject());
    }

    public byte[] objToBytes(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    public static Value fromObject(Object object) {
        if (object instanceof Double ||
                object instanceof Float ||
                object instanceof Integer ||
                object instanceof Long ||
                object instanceof Short ||
                object instanceof Byte) {
            return new Value(Double.parseDouble(object.toString()));
        } else if (object instanceof String) {
            return new Value((String) object);
        } else if (object instanceof Boolean) {
            return new Value((Boolean) object);
        } else if (object instanceof List) {
            List<Value> list = new ArrayList<>();
            for (Object o : (List<Object>) object) {
                list.add(fromObject(o));
            }
            return new Value(list);
        } else if (object instanceof Map) {
            Map<Value, Value> map = new HashMap<>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) object).entrySet()) {
                map.put(fromObject(entry.getKey()), fromObject(entry.getValue()));
            }
            return new Value(map);
        } else if (object instanceof LanguageClass) {
            return new Value((LanguageClass) object);
        } else if (object instanceof Instance) {
            return new Value((Instance) object);
        } else if (object instanceof LanguageEnumChild) {
            return new Value((LanguageEnumChild) object);
        } else if (object instanceof LanguageEnum) {
            return new Value((LanguageEnum) object);
        } else if (object instanceof Spread) {
            return new Value((Spread) object);
        } else if (object instanceof Value) {
            return (Value) object;
        } else if (object instanceof byte[]) {
            return new Value((byte[]) object);
        }
        return new Value();
    }

    public static NativeResult fromByte(byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream is = new ObjectInputStream(in);
            Object obj = is.readObject();
            return NativeResult.Ok(Value.fromObject(obj));
        } catch (IOException | ClassNotFoundException e) {
            return NativeResult.Err("Internal", "Could not read bytes (" + e.getMessage() + ")");
        }
    }

    public Object asObject() {
        if (isInstance)
            return instance;
        else if (isClass)
            return languageClass;
        else if (isEnumParent)
            return enumParent;
        else if (isEnumChild)
            return enumChild;
        else if (isSpread)
            return spread;
        else if (isRef)
            return ref.asObject();
        else if (isBytes)
            return bytes;
        else if (isString)
            return string;
        else if (isNumber)
            return number;
        else if (isBool)
            return bool;
        else if (isList) {
            List<Object> list = new ArrayList<>();
            for (Value value : this.list) {
                list.add(value.asObject());
            }
            return list;
        } else if (isMap) {
            Map<Object, Object> map = new HashMap<>();
            for (Map.Entry<Value, Value> entry : this.map.entrySet()) {
                map.put(entry.getKey().asObject(), entry.getValue().asObject());
            }
            return map;
        }
        return this;
    }

    public String asPatternBinding() {
        return patternBinding;
    }

    public Pattern asPattern() {
        return pattern;
    }

    public Value shallowCopy() {
        if (isNumber) {
            return new Value(number);
        } else if (isString) {
            return new Value(string);
        } else if (isBool) {
            return new Value(bool);
        } else if (isList) {
            return new Value(new ArrayList<>(list));
        } else if (isMap) {
            return new Value(new HashMap<>(map));
        } else if (isClass) {
            return new Value(languageClass.copy());
        } else if (isInstance) {
            return new Value(instance.copy());
        }
        return this;
    }

    public static int[] dumpString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int[] result = new int[bytes.length + 2];
        result[0] = ChunkCode.String;
        result[1] = bytes.length;
        for (int i = 0; i < bytes.length; i++) {
            result[i + 2] = ((int) bytes[i]) << 2;
        }
        return result;
    }

    public static void addAllString(List<Integer> list, String s) {
        for (int i : dumpString(s)) {
            list.add(i);
        }
    }

    public int[] dump() {
        if (isBool) {
            return new int[]{ChunkCode.Boolean, bool ? 1 : 0};
        } else if (isNumber) {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putDouble(number);
            return new int[]{ChunkCode.Number, buffer.getInt(0), buffer.getInt(4)};
        } else if (isString) {
            return dumpString(string);
        } else if (isEnumParent) {
            return enumParent.dump();
        } else if (isFunc) {
            return func.dump();
        }
        return null;
    }

    public Value get(Value other) {
        Map<Value, Value> map = asMap();
        for (Value key : map.keySet()) {
            if (other.equals(key)) {
                return map.get(key);
            }
        }
        return new Value();
    }

    public String type() {
        if (isBool) {
            return "bool";
        } else if (isNumber) {
            return (number == (long) number) ? "i32" : "f32";
        } else if (isString) {
            return "String";
        } else if (isList) {
            return "list";
        } else if (isMap) {
            return "map";
        } else if (isClass) {
            return "recipe";
        } else if (isInstance) {
            return instance.type();
        } else if (isEnumParent) {
            return "Enum";
        } else if (isEnumChild) {
            return enumChild.type();
        } else if (isSpread) {
            return "spread";
        } else if (isRef) {
            return "[" + ref.type() + "]";
        } else if (isBytes) {
            return "bytearray";
        } else if (isFunc || isClosure || isNativeFunc) {
            return "inline";
        } else if (isPattern) {
            return "pattern";
        } else if (isPatternBinding) {
            return "patternBinding";
        } else if (isNamespace) {
            return "namespace";
        } else if (isTuple) {
            return "(" + Arrays.stream(tuple).map(Value::type).collect(Collectors.joining(", ")) + ")";
        }
        return "void";
    }
}
