package language.backend.compiler.bytecode.values.bytecode;

import language.utils.Pair;
import language.backend.compiler.bytecode.values.Value;

public class Result {
    Pair<String, String> error = null;
    Value val;

    public Result(String message, String reason) {
        error = new Pair<>(message, reason);
        val = new Value();
    }

    public Result(Value val) {
        this.val = val;
    }

    public boolean isError() {
        return error != null;
    }

    public String getErrorMessage() {
        return error.getFirst();
    }

    public String getErrorReason() {
        return error.getLast();
    }

    public Value getValue() {
        return val;
    }

}
