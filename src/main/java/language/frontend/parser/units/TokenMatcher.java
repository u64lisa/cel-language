package language.frontend.parser.units;

import language.frontend.lexer.token.TokenType;

public record TokenMatcher(TokenType type, String value) {

    @Override
    public String value() {
        return value;
    }

    @Override
    public TokenType type() {
        return type;
    }
}
