package assembler;

import java.util.List;

public class AssemblerInstruction {
    final public List<String> args;
    final int lineNumber;


    AssemblerInstruction(List<String> args, int lineNumber) {
        this.args = args;
        this.lineNumber = lineNumber;
    }
}
