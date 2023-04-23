package language.backend.compiler.asm;

import language.backend.compiler.asm.inst.Parameter;

public enum AsmRegister {
    AX, CX, DX, BX, SP, BP, SI, DI, R8, R9, R10, R11, R12, R13, R14, R15;

    public static final int BIT_64 = 8,
            BIT_32 = 4,
            BIT_16 = 2,
            BIT_8 = 1;

    public boolean isExtended() {
        return switch (this) {
            case R8, R9, R10, R11, R12, R13, R14, R15 -> true;
            default -> false;
        };
    }

    public String toString(Parameter param) {
        return toString(AsmUtils.getTypeByteSize(param));
    }

    public String toString(int bytes) {
        String name = name();
        if (isExtended()) {
            return switch (bytes) {
                case BIT_64 -> name;
                case BIT_32 -> name + 'D';
                case BIT_16 -> name + 'W';
                case BIT_8 -> name + 'L';
                default -> throw new UnsupportedOperationException();
            };
        } else {
            return switch (bytes) {
                case BIT_64 -> 'R' + name;
                case BIT_32 -> 'E' + name;
                case BIT_16 -> name;
                case BIT_8 -> switch (this) {
                    case AX, CX, DX, BX -> name.substring(0, 1) + 'L';
                    default -> name + 'L';
                };
                default -> throw new UnsupportedOperationException();
            };
        }
    }
}