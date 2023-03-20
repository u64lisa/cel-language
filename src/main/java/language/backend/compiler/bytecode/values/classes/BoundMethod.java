package language.backend.compiler.bytecode.values.classes;

import language.backend.compiler.bytecode.values.bytecode.Closure;
import language.backend.compiler.bytecode.values.Value;

public class BoundMethod {
    public final Closure closure;
    public final Value receiver;

    public BoundMethod(Closure closure, Value receiver) {
        this.closure = closure;
        this.receiver = receiver;
    }

    public String toString() {
        return closure.toString();
    }
}
