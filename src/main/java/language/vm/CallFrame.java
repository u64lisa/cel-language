package language.vm;

import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.bytecode.Closure;

public class CallFrame {
    public final Closure closure;
    public int ip;
    public int slots;
    public String returnType;
    public Value bound;
    public int optimization = 0;
    public boolean catchError = false;
    public boolean addPeek = false;

    public CallFrame(Closure closure, int ip, int slots, String returnType) {
        this(closure, ip, slots, returnType, null);
    }

    public CallFrame(Closure closure, int ip, int slots, String returnType, Value binding) {
        this.closure = closure;
        this.ip = ip;
        this.slots = slots;
        this.returnType = returnType;
        this.bound = binding;
    }
}
