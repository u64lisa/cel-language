package dtool.config.syntax;

import dtool.config.syntax.utils.ISyntaxPos;

import java.util.Objects;

public class DtoolToken {
    public final ISyntaxPos syntaxPosition;
    public final String value;
    public final DtoolTokenType tokenType;

    public DtoolToken(DtoolTokenType tokenType, String value, ISyntaxPos syntaxPosition) {
        this.syntaxPosition = Objects.requireNonNull(syntaxPosition);
        this.value = Objects.requireNonNull(value);
        this.tokenType = Objects.requireNonNull(tokenType);
    }

    public DtoolToken(DtoolTokenType tokenType, ISyntaxPos syntaxPosition) {
        this(tokenType, "", syntaxPosition);
    }

    public String toString() {
        return "{ type: " + tokenType + ", value: '" + value + "' }";
    }

}
