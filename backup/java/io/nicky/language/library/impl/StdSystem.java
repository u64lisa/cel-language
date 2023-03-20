package language.vm.library.impl;

import assembler.Assembler;
import assembler.Bytecode;
import assembler.Instruction;
import assembler.emulation.Memory;
import assembler.emulation.ProcessorX86;
import assembler.exception.EvalException;
import assembler.exception.ParserException;
import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

import java.util.List;

@LibraryClass(className = "System")
public class StdSystem {

    private static final boolean DEBUG = false;

    private final Memory memory = new Memory(1048576);
    private final ProcessorX86 cpu = new ProcessorX86(memory, 1024);
    private final Assembler asm = new Assembler(cpu, memory);

    @LibraryMethod
    public void println(final Object message) {
        System.out.println(message);
    }

    @LibraryMethod
    public void print(final Object message) {
        System.out.print(message);
    }

    @LibraryMethod
    public void err(final Object message) {
        System.err.println(message);
    }

    @LibraryMethod
    public long epoch() {
        return System.currentTimeMillis();
    }

    @LibraryMethod
    public String str(final Object value) {
        return value.toString();
    }

    @LibraryMethod
    public void exit(final int value) {
        System.exit(value);
    }

    @LibraryMethod
    public void compileASM(final Object instructionList) {
        final StringBuilder instructionBuffer = new StringBuilder();

        if (instructionList instanceof List<?> elements) {
            for (Object element : elements) {
                instructionBuffer
                        .append(element)
                        .append("\n");
            }
        }
        if (instructionList instanceof String value) {
            instructionBuffer.append(value);
        }

        final String instructions = instructionBuffer.toString();
        if (instructions.isEmpty() || instructions.equalsIgnoreCase(" "))
            return;

        try {
            Bytecode bytecode = asm.compile(instructions);

            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                int pc = bytecode.pc;
                while (pc < bytecode.length) {
                    Instruction.print(cpu, sb, bytecode.instruction[pc++]);
                }

            }

            cpu.execute(bytecode);
        } catch (ParserException e) {
            System.err.println("Parsing Error >> " + e.getMessage());
        } catch (EvalException e) {
            System.err.println("Evaluation Error >> " + e.getMessage());
        }
    }


}
