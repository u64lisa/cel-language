package assembler.emulation;

import assembler.Bytecode;
import assembler.Opcodes;
import assembler.exception.EvalException;

public class ProcessorX86 extends Processor {

    private static final int WORD_SIZE = 32; /* 32 bits per word */

    private final Register[] registers;
    private final Register sp, pc, r, h;

    private final Memory memory;

    private final ProcessorInstruction currentInstruction;

    public ProcessorX86(Memory memory, int stackSize) {
        if (stackSize > memory.sizeInBytes()) {
            throw new IllegalArgumentException("Stack size is bigger than RAM amount");
        }

        this.memory = memory;

        this.registers = new Register[12];
        this.registers[0] = new Register("$sp", this);
        this.registers[1] = new Register("$pc", this);
        this.registers[2] = new Register("$r", this);
        this.registers[3] = new Register("$h", this);

        this.registers[4] = new Register("$a", this);
        this.registers[5] = new Register("$b", this);
        this.registers[6] = new Register("$c", this);
        this.registers[7] = new Register("$d", this);

        this.registers[8] = new Register("$i", this);
        this.registers[9] = new Register("$j", this);
        this.registers[10] = new Register("$k", this);
        this.registers[11] = new Register("$u", this);

        this.sp = this.registers[0];
        this.pc = this.registers[1];
        this.r = this.registers[2];
        this.h = this.registers[3];

        this.currentInstruction = new ProcessorInstruction(this);

        // Stack grows down, and the Heap grows up
        this.sp.address(memory.sizeInBytes() - 1);
    }

    @Override
    protected EvalException error(String fmt, Object... args) {
        final String str = String.format(fmt, args);
        return new EvalException(str);
    }

    @Override
    public void execute(Bytecode bytecode) {
        int pc = bytecode.pc;
        final int len = bytecode.length;

        final int[] constants = bytecode.constants;
        final int[] instruction = bytecode.instruction;

        this.currentInstruction.constants = constants;

        while (pc < len) {
            int instr = instruction[pc++];
            this.pc.address(pc);

            final int opcode = this.currentInstruction.process(instr);

            switch (opcode) {
                case Opcodes.NOOP -> {}
                case Opcodes.MOVI -> this.currentInstruction
                        .setArg1IntValue(this.currentInstruction.getArg2IntValue());
                case Opcodes.MOVF -> this.currentInstruction
                        .setArg1FloatValue(this.currentInstruction.getArg2FloatValue());
                case Opcodes.MOVB -> this.currentInstruction
                        .setArg1ByteValue(this.currentInstruction.getArg2ByteValue());
                case Opcodes.LDCI -> this.currentInstruction
                        .setArg1IntValue(this.currentInstruction.getConstantIntValue());
                case Opcodes.LDCF -> this.currentInstruction
                        .setArg1FloatValue(this.currentInstruction.getConstantFloatValue());
                case Opcodes.LDCB -> this.currentInstruction
                        .setArg1ByteValue(this.currentInstruction.getConstantByteValue());
                case Opcodes.LDCA -> this.currentInstruction
                        .setArg1IntValue(this.currentInstruction.getConstantAddressValue());
                case Opcodes.PUSHI -> {
                    int value = this.currentInstruction.getArg2IntValue();

                    this.sp.decAddress();
                    setIntValue(this.sp, value);
                }
                case Opcodes.PUSHF -> {
                    float value = this.currentInstruction.getArg2FloatValue();

                    this.sp.decAddress();
                    setFloatValue(this.sp, value);
                }
                case Opcodes.PUSHB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();

                    this.sp.addressOffset(-1);
                    setByteValue(this.sp, value);
                }
                case Opcodes.POPI -> {
                    int value = getIntValueAt(this.sp);
                    this.sp.incAddress();

                    this.currentInstruction.setArg1IntValue(value);
                }
                case Opcodes.POPF -> {
                    float value = getFloatValueAt(this.sp);
                    this.sp.incAddress();

                    this.currentInstruction.setArg1FloatValue(value);
                }
                case Opcodes.POPB -> {
                    byte value = getByteValueAt(this.sp);
                    this.sp.address(+1);

                    this.currentInstruction.setArg1ByteValue(value);
                }
                case Opcodes.DUPI -> {
                    int value = getIntValueAt(this.sp);
                    this.sp.decAddress();

                    setIntValue(this.sp, value);

                    this.currentInstruction.setArg1IntValue(value);
                }
                case Opcodes.DUPF -> {
                    float value = getFloatValueAt(this.sp);
                    this.sp.decAddress();

                    setFloatValue(this.sp, value);

                    this.currentInstruction.setArg1FloatValue(value);
                }
                case Opcodes.DUPB -> {
                    byte value = getByteValueAt(this.sp);
                    this.sp.addressOffset(-1);

                    setByteValue(this.sp, value);

                    this.currentInstruction.setArg1ByteValue(value);
                }
                case Opcodes.IFI -> {
                    int yValue = this.currentInstruction.getArg2IntValue();
                    int xValue = this.currentInstruction.getArg1IntValue();

                    if (xValue > yValue) {
                        pc++;
                    }
                }
                case Opcodes.IFF -> {
                    float yValue = this.currentInstruction.getArg2FloatValue();
                    float xValue = this.currentInstruction.getArg1FloatValue();

                    if (xValue > yValue) {
                        pc++;
                    }
                }
                case Opcodes.IFB -> {
                    byte yValue = this.currentInstruction.getArg2ByteValue();
                    byte xValue = this.currentInstruction.getArg1ByteValue();

                    if (xValue > yValue) {
                        pc++;
                    }
                }
                case Opcodes.IFEI -> {
                    int yValue = this.currentInstruction.getArg2IntValue();
                    int xValue = this.currentInstruction.getArg1IntValue();

                    if (xValue >= yValue) {
                        pc++;
                    }
                }
                case Opcodes.IFEF -> {
                    float yValue = this.currentInstruction.getArg2FloatValue();
                    float xValue = this.currentInstruction.getArg1FloatValue();

                    if (xValue >= yValue) {
                        pc++;
                    }
                }
                case Opcodes.IFEB -> {
                    byte yValue = this.currentInstruction.getArg2ByteValue();
                    byte xValue = this.currentInstruction.getArg1ByteValue();

                    if (xValue >= yValue) {
                        pc++;
                    }
                }
                case Opcodes.JMP -> pc = this.currentInstruction.arg2Value;
                case Opcodes.PRINTI -> System.out.println(this.currentInstruction.getArg2IntValue());
                case Opcodes.PRINTF -> System.out.println(this.currentInstruction.getArg2FloatValue());
                case Opcodes.PRINTB -> System.out.println(this.currentInstruction.getArg2ByteValue());
                case Opcodes.PRINTC -> System.out.print((char) this.currentInstruction.getArg2ByteValue());
                case Opcodes.EL -> System.out.print('\n');
                case Opcodes.CALL -> {
                    this.r.address(pc);

                    pc = this.currentInstruction.arg2Value;
                }
                case Opcodes.RET -> pc = this.r.address();


                /* ===================================================
                 * ALU operations
                 * ===================================================
                 */

                case Opcodes.ADDI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() + value);
                }
                case Opcodes.ADDF -> {
                    float value = this.currentInstruction.getArg2FloatValue();
                    this.currentInstruction.setArg1FloatValue(this.currentInstruction.getArg1FloatValue() + value);
                }
                case Opcodes.ADDB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() + value));
                }
                case Opcodes.SUBI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() - value);
                }
                case Opcodes.SUBF -> {
                    float value = this.currentInstruction.getArg2FloatValue();
                    this.currentInstruction.setArg1FloatValue(this.currentInstruction.getArg1FloatValue() - value);
                }
                case Opcodes.SUBB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() - value));
                }
                case Opcodes.MULI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() * value);
                }
                case Opcodes.MULF -> {
                    float value = this.currentInstruction.getArg2FloatValue();
                    this.currentInstruction.setArg1FloatValue(this.currentInstruction.getArg1FloatValue() * value);
                }
                case Opcodes.MULB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() * value));
                }
                case Opcodes.DIVI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    if (value == 0) {
                        throw error("Divide be zero error.");
                    }

                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() / value);
                }
                case Opcodes.DIVF -> {
                    float value = this.currentInstruction.getArg2FloatValue();
                    if (value == 0) {
                        throw error("Divide be zero error.");
                    }
                    this.currentInstruction.setArg1FloatValue(this.currentInstruction.getArg1FloatValue() / value);
                }
                case Opcodes.DIVB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    if (value == 0) {
                        throw error("Divide be zero error.");
                    }

                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() / value));
                }
                case Opcodes.MODI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    if (value == 0) {
                        throw error("Divide be zero error.");
                    }

                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() % value);
                }
                case Opcodes.MODF -> {
                    float value = this.currentInstruction.getArg2FloatValue();
                    if (value == 0) {
                        throw error("Divide be zero error.");
                    }
                    this.currentInstruction.setArg1FloatValue(this.currentInstruction.getArg1FloatValue() % value);
                }
                case Opcodes.MODB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    if (value == 0) {
                        throw error("Divide be zero error.");
                    }

                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() % value));
                }
                case Opcodes.ORI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() | value);
                }
                case Opcodes.ORB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() | value));
                }
                case Opcodes.ANDI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() & value);
                }
                case Opcodes.ANDB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() & value));
                }
                case Opcodes.NOTI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(~value);
                }
                case Opcodes.NOTB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (~value));
                }
                case Opcodes.XORI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() ^ value);
                }
                case Opcodes.XORB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() ^ value));
                }
                case Opcodes.SZRLI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() >>> value);
                }
                case Opcodes.SZRLB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() >>> value));
                }
                case Opcodes.SRLI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() >> value);
                }
                case Opcodes.SRLB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() >> value));
                }
                case Opcodes.SLLI -> {
                    int value = this.currentInstruction.getArg2IntValue();
                    this.currentInstruction.setArg1IntValue(this.currentInstruction.getArg1IntValue() << value);
                }
                case Opcodes.SLLB -> {
                    byte value = this.currentInstruction.getArg2ByteValue();
                    this.currentInstruction.setArg1ByteValue((byte) (this.currentInstruction.getArg1ByteValue() << value));
                }
                default -> throw error("Unknown opcode: %d", opcode);
            }
        }
    }

    @Override
    public int getIntValueAt(Register r) {
        return this.memory.readInt(r.address());
    }

    @Override
    public float getFloatValueAt(Register r) {
        return this.memory.readFloat(r.address());
    }

    @Override
    public byte getByteValueAt(Register r) {
        return this.memory.readByte(r.address());
    }

    @Override
    public void setIntValue(Register r, int value) {
        this.memory.storeInt(r.address(), value);
    }

    @Override
    public void setFloatValue(Register r, float value) {
        this.memory.storeFloat(r.address(), value);
    }

    @Override
    public void setByteValue(Register r, byte value) {
        this.memory.storeByte(r.address(), value);
    }

    @Override
    public int getWordSize() {
        return WORD_SIZE;
    }

    @Override
    public Register[] getRegisters() {
        return this.registers;
    }

    @Override
    public Register getH() {
        return h;
    }

    @Override
    public Memory getMemory() {
        return memory;
    }

}
