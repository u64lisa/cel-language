package assembler;

public class Opcodes {

    //@formatter:off
    public static final int
        NOOP        = 0,

        MOVI        = 1,   // Moves the int value to the first register MOVI $a $b ($a = $b)
        MOVF        = 2,   // Moves the float value to the first register MOVF $a $b ($a = $b)
        MOVB        = 3,   // Moves the byte value to the first register MOVB $a $b ($a = $b)

        LDCI        = 4,   // Loads a int constant into a register LDCI $a
        LDCF        = 5,   // Loads a float constant into a register LDCF $a
        LDCB        = 6,   // Loads a byte of constant into a register LDCB $a
        LDCA        = 7,   // Loads the address of the constant into a register LDCA $a

        PUSHI       = 8,   // PUSH's the register value onto the stack PUSHI $a
        PUSHF       = 9,   // PUSH's the register value onto the stack PUSHF $a
        PUSHB       = 10,  // PUSH's the register value onto the stack PUSHB $a

        POPI        = 11,  // POP's the top of the stack into the register POPI $a
        POPF        = 12,  // POP's the top of the stack into the register POPF $a
        POPB        = 13,  // POP's the top of the stack into the register POPB $a

        DUPI        = 14,  // Duplicates the top of the stack, stores the top in register $a
        DUPF        = 15,  // Duplicates the top of the stack, stores the top in register $a
        DUPB        = 16,  // Duplicates the top of the stack, stores the top in register $a

        IFI         = 17,  // If (integer) $a > $b skips the next instruction; IFI $a $b
        IFF         = 18,  // If (float)   $a > $b skips the next instruction; IFF $a $b
        IFB         = 19,  // If (byte)    $a > $b skips the next instruction; IFB $a $b

        IFEI        = 20,  // If (integer) $a >= $b skips the next instruction; IFEI $a $b
        IFEF        = 21,  // If (float)   $a >= $b skips the next instruction; IFEF $a $b
        IFEB        = 22,  // If (byte)    $a >= $b skips the next instruction; IFEB $a $b

        JMP         = 23,  // Jumps the amount of $b

        PRINTI      = 24,  // Prints registers
        PRINTF      = 25,  // Prints registers
        PRINTB      = 26,  // Prints the byte at the supplied address
        PRINTC      = 27,  // Prints the byte (as a character) at the supplied address

        CALL        = 28,  // CALL $b  pushes the address on the next instruction onto the stack
        RET         = 29,  // stores return value in $x registers and moves the PC to the the value on the top of the stack


        ADDI        = 30,  // Adds two integers ADDI $a $b => $a = $a + $b
        ADDF        = 31,  // Adds two floats ADDF $a $b => $a = $a + $b
        ADDB        = 32,  // Adds two bytes ADDB $a $b => $a = $a + $b

        SUBI        = 33,  // Subtracts two integers SUBI $a $b => $a = $a - $b
        SUBF        = 34,  // Subtracts two floats SUBF $a $b => $a = $a - $b
        SUBB        = 35,  // Subtracts two bytes SUBB $a $b => $a = $a - $b

        MULI        = 36,  // Multiplies two integers MULI $a $b => $a = $a * $b
        MULF        = 37,  // Multiplies two floats MULF $a $b => $a = $a * $b
        MULB        = 38,  // Multiplies two bytes MULB $a $b => $a = $a * $b

        DIVI        = 39,  // Divides two integers DIVI $a $b => $a = $a / $b
        DIVF        = 40,  // Divides two floats DIVF $a $b => $a = $a / $b
        DIVB        = 41,  // Divides two bytes DIVB $a $b => $a = $a / $b

        MODI        = 42,  // Remainder of two integers MODI $a $b => $a = $a % $b
        MODF        = 43,  // Remainder of two floats MODI $a $b => $a = $a % $b
        MODB        = 44,  // Remainder of two bytes MODI $a $b => $a = $a % $b

        ORI         = 45,  // Bitwise OR of two integers ORI $a $b => $a = $a | $b
        ORB         = 46,  // Bitwise OR of two bytes ORB $a $b => $a = $a | $b

        ANDI        = 47,  // Bitwise AND of two integers ANDI $a $b => $a = $a & $b
        ANDB        = 48,  // Bitwise AND of two bytes ANDB $a $b => $a = $a & $b

        NOTI        = 49,  // Bitwise NOT of the integer NOTI $a $b => $a = ~$b
        NOTB        = 50,  // Bitwise NOT of the byte NOTB $a $b => $a = ~$b

        XORI        = 51,  // Bitwise exclusive OR of the integers XORI $a $b => $a = $a ^ $b
        XORB        = 52,  // Bitwise exclusive OR of the bytes XORI $a $b => $a = $a ^ $b

        SZRLI       = 53,  // Bitwise shift zero right logical operator for integer SZRLI $a $b => $a >>> $b
        SZRLB       = 54,  // Bitwise shift zero right logical operator for byte SZRLB $a $b => $a >>> $b

        SRLI        = 55,  // Bitwise shift right logical operator for integer SRLI $a $b => $a >> $b
        SRLB        = 56,  // Bitwise shift right logical operator for byte SRLB $a $b => $a >> $b

        SLLI        = 57,  // Bitwise shift left logical operator for integer SLLI $a $b => $a << $b
        SLLB        = 58,  // Bitwise shift left logical operator for byte SLLB $a $b => $a << $b

        EL          = 59   // Line ending writes \n
    ;

    private static final Opcode[] opcodesStr = new Opcode[64];
    static {
        opcodesStr[NOOP] = new Opcode("NOOP", 0);

        opcodesStr[MOVI] = new Opcode("MOVI", 2);
        opcodesStr[MOVF] = new Opcode("MOVF", 2);
        opcodesStr[MOVB] = new Opcode("MOVB", 2);

        opcodesStr[LDCI] = new Opcode("LDCI", 2);
        opcodesStr[LDCF] = new Opcode("LDCF", 2);
        opcodesStr[LDCB] = new Opcode("LDCB", 2);
        opcodesStr[LDCA] = new Opcode("LDCA", 2);

        opcodesStr[PUSHI] = new Opcode("PUSHI", 1);
        opcodesStr[PUSHF] = new Opcode("PUSHF", 1);
        opcodesStr[PUSHB] = new Opcode("PUSHB", 1);

        opcodesStr[POPI] = new Opcode("POPI", 1);
        opcodesStr[POPF] = new Opcode("POPF", 1);
        opcodesStr[POPB] = new Opcode("POPB", 1);

        opcodesStr[DUPI] = new Opcode("DUPI", 1);
        opcodesStr[DUPF] = new Opcode("DUPF", 1);
        opcodesStr[DUPB] = new Opcode("DUPB", 1);

        opcodesStr[IFI] = new Opcode("IFI", 2);
        opcodesStr[IFF] = new Opcode("IFF", 2);
        opcodesStr[IFB] = new Opcode("IFB", 2);

        opcodesStr[IFEI] = new Opcode("IFEI", 2);
        opcodesStr[IFEF] = new Opcode("IFEF", 2);
        opcodesStr[IFEB] = new Opcode("IFEB", 2);

        opcodesStr[JMP] = new Opcode("JMP", 1);

        opcodesStr[PRINTI] = new Opcode("PRINTI", 1);
        opcodesStr[PRINTF] = new Opcode("PRINTF", 1);
        opcodesStr[PRINTB] = new Opcode("PRINTB", 1);
        opcodesStr[PRINTC] = new Opcode("PRINTC", 1);

        opcodesStr[CALL] = new Opcode("CALL", 1);
        opcodesStr[RET]  = new Opcode("RET", 0);



        opcodesStr[ADDI] = new Opcode("ADDI", 2);
        opcodesStr[ADDF] = new Opcode("ADDF", 2);
        opcodesStr[ADDB] = new Opcode("ADDB", 2);

        opcodesStr[SUBI] = new Opcode("SUBI", 2);
        opcodesStr[SUBF] = new Opcode("SUBF", 2);
        opcodesStr[SUBB] = new Opcode("SUBB", 2);

        opcodesStr[MULI] = new Opcode("MULI", 2);
        opcodesStr[MULF] = new Opcode("MULF", 2);
        opcodesStr[MULB] = new Opcode("MULB", 2);

        opcodesStr[DIVI] = new Opcode("DIVI", 2);
        opcodesStr[DIVF] = new Opcode("DIVF", 2);
        opcodesStr[DIVB] = new Opcode("DIVB", 2);

        opcodesStr[MODI] = new Opcode("MODI", 2);
        opcodesStr[MODF] = new Opcode("MODF", 2);
        opcodesStr[MODB] = new Opcode("MODB", 2);

        opcodesStr[ORI] = new Opcode("ORI", 2);
        opcodesStr[ORB] = new Opcode("ORB", 2);

        opcodesStr[ANDI] = new Opcode("ANDI", 2);
        opcodesStr[ANDB] = new Opcode("ANDB", 2);

        opcodesStr[NOTI] = new Opcode("NOTI", 2);
        opcodesStr[NOTB] = new Opcode("NOTB", 2);

        opcodesStr[XORI] = new Opcode("XORI", 2);
        opcodesStr[XORB] = new Opcode("XORB", 2);

        opcodesStr[SZRLI] = new Opcode("SZRLI", 2);
        opcodesStr[SZRLB] = new Opcode("SZRLB", 2);

        opcodesStr[SRLI] = new Opcode("SRLI", 2);
        opcodesStr[SRLB] = new Opcode("SRLB", 2);

        opcodesStr[SLLI] = new Opcode("SLLI", 2);
        opcodesStr[SLLB] = new Opcode("SLLB", 2);

        opcodesStr[EL] = new Opcode("EL", 0);
    }
    //@formatter:on

    public static String opcodeStr(int opcode) {
        return opcodesStr[opcode].opcode;
    }

    public static int strOpcode(String opcode) {
        for (int index = 0; index < opcodesStr.length; index++) {
            if (opcodesStr[index] != null && opcodesStr[index].opcode.equalsIgnoreCase(opcode)) {
                return index;
            }
        }
        return -1;
    }

    public static int numberOfArgs(int opcode) {
        return opcodesStr[opcode].numberOfArgs;
    }
}
