package language.backend.compiler.bytecode.values.classes;

import language.backend.compiler.bytecode.values.Value;

public class ClassAttribute {
    public Value val;
    public final boolean isStatic;
    public final boolean isPrivate;

    public ClassAttribute(Value val, boolean isStatic, boolean isPrivate) {
        this.val = val;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
    }

    public ClassAttribute(Value val) {
        this(val, false, false);
    }

    public void set(Value val) {
        this.val = val;
    }

}
