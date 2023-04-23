package language.backend.compiler.asm.inst;

import dtool.config.syntax.utils.StringUtils;
import language.backend.compiler.asm.types.ASMTypeLookup;
import language.backend.compiler.asm.types.ValueType;
import language.frontend.lexer.token.Token;

public class Parameter {

    private final Token name;

    private final Type type;
    protected ValueType valueType;

    public Parameter(Token name, Type type, ValueType valueType) {
        this.name = name;
        this.type = type;
        this.valueType = valueType;
    }

    public Parameter(Token name, Type type) {
        this.name = name;
        this.type = type;
    }

    public static enum Type {
        REF, NUM, STR
    }

    public Token getName() {
        return name;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "name=" + name +
                ", type=" + type +
                ", valueType=" + valueType +
                '}';
    }

    public static class ReferenceParameter extends Parameter {

        private final ValueType valueType;
        private final int id;
        private int flags;

        public ReferenceParameter(Token name, ValueType valueType, int id, int flags) {
            super(name, Type.REF);

            this.valueType = valueType;
            this.id = id;
            this.flags = flags;
        }

        @Override
        public ValueType getValueType() {
            return valueType;
        }

        public int getId() {
            return id;
        }

        public int getFlags() {
            return flags;
        }
    }

    public static class StringParameter extends Parameter {

        private final String value;

        public StringParameter(Token name, String value) {
            super(name, Type.STR);
            this.value = value;
        }

        public ValueType getSize() {
            return ASMTypeLookup.U8.createArray(1);
        }

        public String toString() {
            return "\"%s\"".formatted(StringUtils.escapeString(value));
        }

    }

    public static class NumberParameter extends Parameter {

        private final long value;

        public NumberParameter(Token name, ValueType valueType, long value) {
            super(name, Type.NUM, valueType);
            this.value = value;
        }

        public ValueType getSize() {
            return valueType;
        }

        public long getValue() {
            return value;
        }

        public String toString() {

            int size = valueType.getSize();
            if (valueType.isFloating()) {
                return switch (size) {
                    case 64 -> Double.toString(Double.longBitsToDouble(value));
                    case 32 -> Float.toString(Float.intBitsToFloat((int) value));
                    default -> throw new RuntimeException("Invalid float size %s".formatted(size));
                };
            }

            if (valueType.isUnsigned()) {
                return switch (size) {
                    case 64 -> Long.toUnsignedString(value);
                    case 32 -> Integer.toUnsignedString((int) value);
                    case 16 -> Integer.toString((int) value & 0xffff);
                    case 8 -> Integer.toString((int) value & 0xff);
                    default -> throw new RuntimeException("Invalid unsigned size %s".formatted(size));
                };
            }

            return switch (size) {
                case 64 -> Long.toString(value);
                case 32 -> Integer.toString((int) value);
                case 16 -> Short.toString((short) value);
                case 8 -> Byte.toString((byte) value);
                default -> throw new RuntimeException("Invalid integer size %s".formatted(size));
            };
        }
    }

}
