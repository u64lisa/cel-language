/*
 * see license.txt
 */
package assembler;

import assembler.emulation.ProcessorX86;

public class Instruction {

    public static final int MAX_IMMEDIATE_VALUE = 0b11_1111_1111_1111_1111_1;

    public static final int INSTRUCTION_SIZE = 32;

    public static final int OPCODE_SIZE    = 6;
    public static final int OPCODE_SHIFT   = INSTRUCTION_SIZE - OPCODE_SIZE;
    public static final int OPCODE_MASK    = 0b111111;

    public static final int ARG1_SIZE      = 5;
    public static final int ARG1_SHIFT     = 21;
    public static final int ARG1_MASK      = 0b11111;
    public static final int ARG1_ADDR_MASK = 0b10000;
    public static final int ARG1_VALUE_MASK= 0b01111;

    public static final int ARG2_SIZE      = 21;
    public static final int ARG2_SHIFT     = 0;
    public static final int ARG2_MASK      = 0b1111_1111_1111_1111_1111_1;
    public static final int ARG2_REG_MASK  = 0b1000_0000_0000_0000_0000_0;
    public static final int ARG2_ADDR_MASK = 0b0100_0000_0000_0000_0000_0;
    public static final int ARG2_IMM_MASK  = 0b0100_0000_0000_0000_0000_0;
    public static final int ARG2_VALUE_MASK= 0b0011_1111_1111_1111_1111_1;

    public static final int ARG_JMP_VALUE_MASK= 0b000000_1111_1111_1111_1111_1111_1111;

    public static int opcode(int instruction) {
        return (instruction >>> OPCODE_SHIFT);
    }
    
//    public static boolean isArg1Reg(int instruction) {
//        return ((instruction >> ARG1_SHIFT) & ARG1_REG_MASK) != 0;
//    }

    public static boolean isArg1Addr(int instruction) {
        return ((instruction >> ARG1_SHIFT) & ARG1_ADDR_MASK) != 0;
    }

    public static int arg1Value(int instruction) {
        return ((instruction >> ARG1_SHIFT) & ARG1_VALUE_MASK);
    }


    public static boolean isArg2Reg(int instruction) {
        return ((instruction >> ARG2_SHIFT) & ARG2_REG_MASK) != 0;
    }

    public static boolean isArg2Addr(int instruction) {
        return ((instruction >> ARG2_SHIFT) & ARG2_ADDR_MASK) != 0;
    }

    public static boolean isArg2Immediate(int instruction) {
        return ((instruction >> ARG2_SHIFT) & ARG2_IMM_MASK) != 0;
    }

    public static int arg2Value(int instruction) {
        return ((instruction >> ARG2_SHIFT) & ARG2_VALUE_MASK);
    }

    public static int argJmpValue(int instruction) {
        return instruction & ARG_JMP_VALUE_MASK;
    }


    public static void print(ProcessorX86 cpu, StringBuilder sb, int instruction) {
        int opcode = opcode(instruction);
        sb.append(Opcodes.opcodeStr(opcode)).append(" ");
        
        if(opcode == Opcodes.JMP || opcode == Opcodes.CALL) {
            sb.append("#").append(argJmpValue(instruction));
        }
        else {
            if(Opcodes.numberOfArgs(opcode(instruction)) == 2) {
            
                if(isArg1Addr(instruction)) {
                    sb.append("&");
                }
                
                sb.append("$");
                
                int arg1Register = arg1Value(instruction);
                sb.append(cpu.getRegisters()[arg1Register].getName()).append(" ");
            }
            
            if(isArg2Reg(instruction)) {
                if(isArg2Addr(instruction)) {
                    sb.append("&");
                }
                sb.append("$").append(cpu.getRegisters()[arg2Value(instruction)].getName());
            }
            else if(isArg2Immediate(instruction)) {
                sb.append("#").append(arg2Value(instruction));
            }
            else {
                sb.append(arg2Value(instruction));
            }
        }
        sb.append("\n");
    }
}
