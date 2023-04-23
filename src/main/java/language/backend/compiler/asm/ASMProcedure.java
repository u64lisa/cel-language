package language.backend.compiler.asm;

import language.backend.compiler.asm.inst.Parameter;

public class ASMProcedure {


    private int stackSize;
    private int offset;

    public int getStackSize() {
        return stackSize;
    }

    public int getOffset() {
        return offset;
    }

    public int getStackOffset(Parameter parameter) {
        return 0;
    }
}
