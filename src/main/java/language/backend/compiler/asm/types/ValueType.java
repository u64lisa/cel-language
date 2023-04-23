package language.backend.compiler.asm.types;

public class ValueType {
    public static final int SIGNED = 0,
            UNSIGNED = 1,
            FLOATING = 2,
            GENERIC = 3,
            LONG = 6;

    public static final int STORAGE_TYPE = 15,
            CONST = 16;

    private final String name;
    private final int flags;
    private final int depth;
    private final int size;

    public ValueType(String name, int size, int depth, int flags) {
        this.name = name;
        this.size = size;
        this.depth = depth;
        this.flags = flags;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getFlags() {
        return flags;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isSigned() {
        return (flags & STORAGE_TYPE) == SIGNED;
    }

    public boolean isFloating() {
        return (flags & STORAGE_TYPE) == FLOATING;
    }

    public boolean isUnsigned() {
        return (flags & STORAGE_TYPE) == UNSIGNED;
    }

    public boolean isLong() {
        return (flags & STORAGE_TYPE) == LONG;
    }

    public int calculateBytes() {
        return (getDepth() > 0) ? getPointerSize() : (getSize() >> 3);
    }

    public static int getPointerSize() {
        return 8;
    }

    public ValueType createArray(int i) {
        return new ValueType(name, size, i, flags);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + flags;
        result = 31 * result + depth;
        result = 31 * result + size;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ValueType that))
            return false;
        return this.getDepth() == that.getDepth()
                && this.getFlags() == that.getFlags()
                && this.getSize() == that.getSize();
    }

    public String toShortName() {
        StringBuilder sb = new StringBuilder();

        if ((flags & CONST) != 0) {
            sb.append("const ");
        }

        switch (flags & STORAGE_TYPE) {
            case SIGNED -> sb.append("i");
            case UNSIGNED -> sb.append("u");
            case FLOATING -> sb.append("f");
            default -> sb.append("unk");
        }

        return sb.append(size).append("[]".repeat(depth)).toString();
    }

    @Override
    public String toString() {
        return toShortName();
    }


}