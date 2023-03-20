/*
 * see license.txt
 */
package assembler;

import java.util.Arrays;

public class Bytecode {

    // array index is the Constant ID, and the
    // int value is the address of where the constant
    // is stored in RAM
    public final int[] constants;

    public final int[] instruction;
    public int length;
    public int pc;

    public Bytecode(int[] constants, int[] instruction, int pc, int length) {
        this.constants = constants;
        this.instruction = instruction;
        this.pc = pc;
        this.length = length;
    }

    @Override
    public String toString() {
        return "Bytecode{" +
                "constants=" + Arrays.toString(constants) +
                ", instruction=" + Arrays.toString(instruction) +
                ", length=" + length +
                ", pc=" + pc +
                '}';
    }
}
