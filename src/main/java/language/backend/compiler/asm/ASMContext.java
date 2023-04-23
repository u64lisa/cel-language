package language.backend.compiler.asm;

public class ASMContext {

    private final String tab = "     ";

    private final StringBuilder stringBuilder;

    public ASMContext() {
        this.stringBuilder = new StringBuilder();
    }

    public ASMContext(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public ASMContext aC(final String... comment) {
        for (String line : comment) {
            stringBuilder.append("; ").append(line).append("\n");
        }
        return this;
    }

    public ASMContext aCT(final String... comment) {
        for (String line : comment) {
            stringBuilder.append(tab).append("; ").append(line).append("\n");
        }
        return this;
    }

    public ASMContext aH(final String entry, final String... documentation) {
        for (String line : documentation) {
            stringBuilder.append("; ").append(line).append("\n");
        }
        stringBuilder.append(entry).append("\n");
        return this;
    }

    public ASMContext aI(final String entry, final String... comment) {
        for (String line : comment) {
            stringBuilder.append(tab).append("; ").append(line).append("\n");
        }
        stringBuilder.append(tab).append(entry).append("\n");
        return this;
    }

    public ASMContext nl() {
        stringBuilder.append("\n");

        return this;
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

}
