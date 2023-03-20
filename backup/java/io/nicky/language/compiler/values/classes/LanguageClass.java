package language.backend.compiler.bytecode.values.classes;

import language.backend.compiler.bytecode.values.bytecode.Closure;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.bytecode.NativeResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static language.backend.compiler.bytecode.values.classes.Instance.copyAttributes;

public class LanguageClass {
    public final LanguageClass superClass;

    public final String name;
    public final Map<String, ClassAttribute> attributes;
    public Map<String, Value> methods;
    public Map<String, Value> binMethods;
    public List<String> generics;

    public Value constructor;
    public Type type;

    public LanguageClass(String name, Map<String, ClassAttribute> attributes,
                         List<String> generics, LanguageClass superClass) {
        this.name = name;
        this.attributes = new HashMap<>();
        this.methods = new HashMap<>();
        this.binMethods = new HashMap<>();

        this.superClass = superClass;
        if (superClass != null) {
            copyAttributes(superClass.attributes, this.attributes);
            this.methods = superClass.methods;
            this.binMethods = superClass.binMethods;
        }

        this.attributes.putAll(attributes);
        this.generics = generics;
    }

    public String toString() {
        return name;
    }

    public void addMethod(String name, Value value) {
        if (name.equals("<make>"))
            constructor = value;
        else if (value.asClosure().byteCode.isBin)
            binMethods.put(name, value);
        else
            methods.put(name, value);
    }

    public Value getField(String name, boolean internal) {
        ClassAttribute attr = attributes.get(name);
        if (attr != null && attr.isStatic && (!attr.isPrivate || internal))
            return attr.val;

        Value val = methods.get(name);
        Closure method = val != null ? val.asClosure() : null;
        if (method != null && method.byteCode.isStatic && (!method.byteCode.isPrivate || internal))
            return val;

        return null;
    }

    public NativeResult setField(String name, Value value) {
        return Instance.setField(name, value, attributes);
    }

    public boolean hasField(String name) {
        return attributes.containsKey(name);
    }

    public boolean has(String name) {
        return attributes.containsKey(name) || methods.containsKey(name);
    }

    public LanguageClass copy() {
        Map<String, ClassAttribute> attrs = new HashMap<>();
        copyAttributes(attributes, attrs);
        return new LanguageClass(name, attrs, generics, superClass.copy());
    }
}
