package assembler.emulation;

import assembler.Instruction;
import assembler.Opcodes;

public class ProcessorInstruction {

    private final ProcessorX86 processorX86;
    int[] constants;

    boolean isReg = false;

    boolean isArg1Address = false;
    boolean isArg2Address = false;
    boolean isImmediate = false;

    Register xPosition = null;
    Register yPosition = null;

    int arg2Value = 0;

    public ProcessorInstruction(ProcessorX86 processorX86) {
        this.processorX86 = processorX86;
    }


    @SuppressWarnings("all")
    int process(int instr) {
        final int opcode = Instruction.opcode(instr);

        isReg = false;

        isArg1Address = false;
        isArg2Address = false;
        isImmediate = false;

        xPosition = null;
        yPosition = null;

        arg2Value = 0;

        ////
        // JMP instruction is special in that its argument is
        // a 24 bit immediate mode number
        ////
        if (opcode == Opcodes.JMP || opcode == Opcodes.CALL) {
            arg2Value = Instruction.argJmpValue(instr);
        } else {
            ////
            // All other instructions follow arg1/arg2 formats
            ////

            if (Opcodes.numberOfArgs(opcode) == 2) {
                xPosition = processorX86.getRegisters()[Instruction.arg1Value(instr)];
                isArg1Address = Instruction.isArg1Addr(instr);

                isReg = Instruction.isArg2Reg(instr);

                // Determine if Arg2 is a Register or Constant value
                if (isReg) {
                    // The register is either an address to a value in memory OR a value
                    yPosition = processorX86.getRegisters()[Instruction.arg2Value(instr)];
                    isArg2Address = Instruction.isArg2Addr(instr);
                } else {
                    // The constant is either an immediate value OR index to RAM
                    isImmediate = Instruction.isArg2Immediate(instr);
                    arg2Value = Instruction.arg2Value(instr);
                }
            } else {
                isReg = Instruction.isArg2Reg(instr);

                // Determine if Arg2 is a Register or Constant value
                if (isReg) {
                    // The register is either an address to a value in memory OR a value
                    yPosition = processorX86.getRegisters()[Instruction.arg2Value(instr)];

                    this.xPosition = yPosition;
                    isArg2Address = Instruction.isArg2Addr(instr);
                } else {
                    // The constant is either an immediate value OR index to RAM
                    isImmediate = Instruction.isArg2Immediate(instr);
                    arg2Value = Instruction.arg2Value(instr);
                }
            }
        }

        return opcode;
    }

    int getArg1IntValue() {
        return isArg1Address ? processorX86.getIntValueAt(xPosition) : xPosition.intValue();
    }

    float getArg1FloatValue() {
        return isArg1Address ? processorX86.getFloatValueAt(xPosition) : xPosition.floatValue();
    }

    byte getArg1ByteValue() {
        return isArg1Address ? processorX86.getByteValueAt(xPosition) : xPosition.byteValue();
    }

    void setArg1IntValue(int value) {
        if (isArg1Address) {
            processorX86.setIntValue(xPosition, value);
        } else {
            xPosition.value(value);
        }
    }

    void setArg1FloatValue(float value) {
        if (isArg1Address) {
            processorX86.setFloatValue(xPosition, value);
        } else {
            xPosition.value(value);
        }
    }

    void setArg1ByteValue(byte value) {
        if (isArg1Address) {
            processorX86.setByteValue(xPosition, value);
        } else {
            xPosition.value(value);
        }
    }

    int getArg2IntValue() {
        return isReg ? (isArg2Address ? processorX86.getIntValueAt(yPosition) : yPosition.intValue())
                : isImmediate ? arg2Value
                : processorX86.getMemory().readInt(constants[arg2Value]);
    }

    byte getArg2ByteValue() {
        return isReg ? (isArg2Address ? processorX86.getByteValueAt(yPosition) : yPosition.byteValue())
                : isImmediate ? (byte) arg2Value
                : processorX86.getMemory().readByte(constants[arg2Value]);
    }

    float getArg2FloatValue() {
        return isReg ? (isArg2Address ? processorX86.getFloatValueAt(yPosition) : yPosition.floatValue())
                : processorX86.getMemory().readFloat(constants[arg2Value]);
    }

    int getConstantIntValue() {
        return isImmediate ? arg2Value
                : processorX86.getMemory().readInt(constants[arg2Value]);
    }

    float getConstantFloatValue() {
        return processorX86.getMemory().readFloat(constants[arg2Value]);
    }

    byte getConstantByteValue() {
        return isImmediate ? (byte) arg2Value
                : processorX86.getMemory().readByte(constants[arg2Value]);
    }

    int getConstantAddressValue() {
        return constants[arg2Value];
    }
}
