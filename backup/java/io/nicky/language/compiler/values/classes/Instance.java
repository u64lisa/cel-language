package language.backend.compiler.bytecode.values.classes;

import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.Var;
import language.backend.compiler.bytecode.values.bytecode.Closure;
import language.backend.compiler.bytecode.values.bytecode.NativeResult;
import language.vm.VirtualMachine;
import language.vm.VirtualMachineResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class Instance {
    public final LanguageClass clazz;
    public final Map<String, ClassAttribute> fields;
    public final Map<String, Value> methods;
    public final Map<String, Value> binMethods;
    public Value self;
    final VirtualMachine virtualMachine;
    final Map<String, String> generics;
    public Type type;

    public Instance(LanguageClass clazz, VirtualMachine virtualMachine) {
        this.clazz = clazz;
        methods = clazz.methods;
        binMethods = clazz.binMethods;
        generics = new HashMap<>();

        this.virtualMachine = virtualMachine;

        fields = new HashMap<>();
        copyAttributes(clazz.attributes, fields);
        methods.putAll(binMethods);
    }

    public Instance(String name, Map<String, ClassAttribute> attrs, VirtualMachine virtualMachine) {
        this(new LanguageClass(name, attrs, new ArrayList<>(), null), virtualMachine);
    }

    public static void copyAttributes(Map<String, ClassAttribute> src, Map<String, ClassAttribute> dst) {
        for (Map.Entry<String, ClassAttribute> entry : src.entrySet()) {
            ClassAttribute value = entry.getValue();
            dst.put(entry.getKey(), new ClassAttribute(
                    value.val,
                    value.isStatic,
                    value.isPrivate
            ));
        }
    }

    public String getGeneric(String key) {
        return generics.get(key);
    }

    private String stringOp(String opName) {
        String res = unfailableOp(opName, clazz.name, "String");
        if (res == null)
            res = virtualMachine.pop().asString();
        return res;
    }

    public boolean instanceOf(Value value) {
        if (value.isEnumChild && hasField("$child") && hasField("$child")) {
            return value.asEnumChild().getValue() == fields.get("$child").val.asNumber().intValue() &&
                    value.asEnumChild().getParent() == fields.get("$parent").val.asEnum();
        }
        else if (value.isClass) {
            return clazz == value.asClass();
        }
        return false;
    }

    public String type() {
        return clazz.name;
    }

    @Override
    public String toString() {
        return stringOp("string");
    }

    private <T> T _unfailableOp(String opName, T def, String type) {
        Value val = binMethods.get(opName);
        if (val != null) {
            virtualMachine.push(new Value(new Var(
                    self,
                    true
            )));
            boolean worked = virtualMachine.call(val.asClosure(), self, new Value[0], new HashMap<>());
            if (!worked)
                return def;

            virtualMachine.frame = virtualMachine.frames.peek();
            virtualMachine.frame.returnType = type;

            VirtualMachineResult res = virtualMachine.run();
            if (res == VirtualMachineResult.ERROR) {
                return def;
            }
            else {
                return null;
            }
        }
        return def;
    }

    private <T> T unfailableOp(String opName, T def, String type) {
        virtualMachine.safe = true;
        T res = _unfailableOp(opName, def, type);
        virtualMachine.safe = false;
        return res;
    }

    public Value getField(String name, boolean internal) {
        ClassAttribute attr = fields.get(name);
        if (attr != null && (!attr.isPrivate || internal))
            return attr.val;

        Value val = methods.get(name);
        Closure method = val != null ? val.asClosure() : null;
        if (method != null && (!method.byteCode.isPrivate || internal))
            return val;

        return null;
    }

    public NativeResult setField(String name, Value value) {
        return setField(name, value, fields);
    }

    public static NativeResult setField(String name, Value value, Map<String, ClassAttribute> fields) {
        ClassAttribute attr = fields.get(name);
        attr.set(value);
        return NativeResult.Ok();
    }

    public Double asNumber() {
        Double res = unfailableOp("number", 0.0, "num");
        if (res == null)
            res = virtualMachine.pop().asNumber();
        return res;
    }

    public boolean asBool() {
        Boolean res = unfailableOp("boolean", true, "bool");
        if (res == null)
            res = virtualMachine.pop().asBool();
        return res;
    }

    public List<Value> asList() {
        List<Value> res = unfailableOp("list", new ArrayList<>(Collections.singletonList(self)), "list");
        if (res == null)
            res = virtualMachine.pop().asList();
        return res;
    }

    public Map<Value, Value> asMap() {
        Map<Value, Value> res = unfailableOp("map", new HashMap<>(Collections.singletonMap(
                self, self
        )), "map");
        if (res == null)
            res = virtualMachine.pop().asMap();
        return res;
    }

    public Instance copy() {
        return new Instance(clazz, virtualMachine);
    }

    public byte[] asBytes() {
        byte[] res = unfailableOp("bytes", objToBytes(self.asObject()), "bytes");
        if (res == null)
            res = virtualMachine.pop().asBytes();
        return res;
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

    public boolean hasField(String key) {
        return fields.containsKey(key);
    }
}
