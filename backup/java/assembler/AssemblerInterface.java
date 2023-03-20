package assembler;

import assembler.emulation.ProcessorX86;
import assembler.emulation.Memory;
import assembler.exception.EvalException;
import assembler.exception.ParserException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class AssemblerInterface {

    public static void main(String[] args) throws IOException {
        testEmulator(false);
    }

    public static void testEmulator(final boolean debugMode) {
        try {
            final InputStream inputStream = new FileInputStream("./src/main/java/assembler/test.asm");

            final Scanner scanner = new Scanner(inputStream);
            final StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine())
                content.append(scanner.nextLine()).append('\n');

            scanner.close();
            emulate(content.toString(), debugMode);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void emulate(final String code, final boolean debugMode) {
        AssemblerInterface vm = new AssemblerInterface(1048576, 1024, debugMode);

        Assembler asm = new Assembler(vm.cpu, vm.memory);
        try {
            Bytecode bytecode = asm.compile(code);

            vm.execute(bytecode);
        } catch (ParserException e) {
            System.err.println("Parsing Error >> " + e.getMessage());
            if (debugMode) {
                throw e;
            }
        } catch (EvalException e) {
            System.err.println("Evaluation Error >> " + e.getMessage());
            if (debugMode) {
                throw e;
            }
        }
    }


    private final Memory memory;
    private final ProcessorX86 cpu;
    private final boolean debugMode;

    public AssemblerInterface(int ramSize, int stackSize, boolean debugMode) {
        if (stackSize > ramSize) {
            throw new IllegalArgumentException("Stack size is bigger than RAM amount");
        }

        this.memory = new Memory(ramSize);
        this.cpu = new ProcessorX86(this.memory, stackSize);

        this.debugMode = debugMode;
    }

    public ProcessorX86 getCpu() {
        return cpu;
    }

    public Memory getRam() {
        return memory;
    }

    public void execute(Bytecode code) {
        if (this.debugMode) {
            printInstructions(code);
        }

        this.cpu.execute(code);
    }

    private void printInstructions(Bytecode code) {
        StringBuilder sb = new StringBuilder();
        int pc = code.pc;
        while (pc < code.length) {
            Instruction.print(cpu, sb, code.instruction[pc++]);
        }

        System.out.println(sb);
    }

}
