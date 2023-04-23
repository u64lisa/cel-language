package language.backend.compiler.llvm;

public class GeneratingState {
    public Function function;
    public long block;
    public GeneratingState(final Function function, final long block) {
        this.function = function;
        this.block = block;
    }
}
