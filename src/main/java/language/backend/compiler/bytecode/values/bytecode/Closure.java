package language.backend.compiler.bytecode.values.bytecode;

import language.backend.compiler.bytecode.values.Var;

public class Closure {
    public final ByteCode byteCode;

    public final Var[] upvalues;
    public int upvalueCount;

    public Closure(ByteCode byteCode) {
        this.byteCode = byteCode;
        this.upvalueCount = byteCode.upvalueCount;
        this.upvalues = new Var[upvalueCount];
    }

    public void asMethod(boolean isStatic, boolean isPrivate, boolean isBin, String owner) {
        byteCode.isStatic = isStatic;
        byteCode.isPrivate = isPrivate;
        byteCode.isBin = isBin;
        byteCode.owner = owner;
    }

    public String toString() {
        return byteCode.toString();
    }
}
