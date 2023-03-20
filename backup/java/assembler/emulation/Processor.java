package assembler.emulation;

import assembler.Bytecode;
import assembler.exception.EvalException;

public abstract class Processor {
    protected abstract EvalException error(String fmt, Object... args);

    public abstract void execute(Bytecode bytecode);

    public abstract int getIntValueAt(Register r);

    public abstract float getFloatValueAt(Register r);

    public abstract byte getByteValueAt(Register r);

    public abstract void setIntValue(Register r, int value);

    public abstract void setFloatValue(Register r, float value);

    public abstract void setByteValue(Register r, byte value);

    public abstract int getWordSize();

    public abstract Register[] getRegisters();

    public abstract Register getH();

    public abstract Memory getMemory();
}
