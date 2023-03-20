package assembler;

public final class Opcode {
    public String opcode;
    public int numberOfArgs;

    Opcode(String opcode, int numberOfArgs) {
        this.opcode = opcode;
        this.numberOfArgs = numberOfArgs;
    }
}