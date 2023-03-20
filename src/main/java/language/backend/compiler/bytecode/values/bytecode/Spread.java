package language.backend.compiler.bytecode.values.bytecode;

import language.backend.compiler.bytecode.values.Value;

import java.util.List;

public class Spread {
    public final List<Value> values;

    public Spread(List<Value> values) {
        this.values = values;
    }
}
