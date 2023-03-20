package language.frontend.lexer.token;

import language.utils.Pair;
import language.utils.position.ISyntaxPosition;
import language.utils.position.MutableSyntaxImpl;


import java.util.List;

public class Token {
    private final TokenType type;
    private final Object value;
    private Position startPosition;
    private Position endPosition;

    public Token(TokenType type, Object value, Position startPosition, Position endPosition) {
        this.type = type;
        this.value = value;

        if (startPosition != null) {
            this.startPosition = startPosition.copy();
            this.endPosition = endPosition != null ? endPosition.copy() : startPosition.copy().advance();
        }
    }

    public Token(TokenType type, Position startPosition) {
        this.type = type;
        this.value = null;

        this.startPosition = startPosition.copy();
        this.endPosition = startPosition.copy().advance();
    }

    public Token(TokenType type, Position startPosition, Position endPosition) {
        this.type = type;
        this.value = null;

        this.startPosition = startPosition.copy();
        this.endPosition = endPosition.copy();
    }

    public boolean matches(TokenType type, Object value) {
        return this.type.equals(type) && (this.value == null || this.value.equals(value));
    }

    public String toString() {
        return value != null ? String.format(
                "%s:%s",
                type, value
        ) : String.valueOf(type);
    }

    public String debugToken() {
        return this.type.toString() + " - " + this.value + " (" + startPosition.toString() + ":" + endPosition.toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Token)) return false;

        Token other = (Token) o;
        if (value == null) return other.type == type && other.value == null;
        return other.type == type && value.equals(other.value);
    }

    public String asString() {
        return switch (type) {
            case TYPE -> String.join("", (List<String>) value);
            case INVISIBLE_NEW_LINE -> "";
            case LEFT_TILDE_ARROW -> "<~";
            case TILDE_TILDE -> "~~";
            case RIGHT_TILDE_ARROW -> "~>";
            case EQUAL -> "=";
            case TILDE_AMPERSAND -> "~&";
            case TILDE_PIPE -> "~|";
            case TILDE_CARET -> "~^";
            case TILDE -> "~";
            case AT -> "@";
            case INTEGER, FLOAT, LONG, DOUBLE, SHORT, BYTE, BOOLEAN -> String.valueOf(value);
            case STRING -> ((Pair<String, Boolean>) value).getFirst();
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case LEFT_PAREN -> "(";
            case RIGHT_PAREN -> ")";
            case EOF -> "EOF";
            case NEW_LINE -> "\n";
            case CARET -> "^";
            case IDENTIFIER, KEYWORD -> (String) value;
            case FAT_ARROW -> "=>";
            case EQUAL_EQUAL -> "==";
            case BANG_EQUAL -> "!=";
            case LEFT_ANGLE -> "<";
            case RIGHT_ANGLE -> ">";
            case LESS_EQUALS -> "<=";
            case GREATER_EQUALS -> ">=";
            case AMPERSAND -> "&";
            case PIPE -> "|";
            case BANG -> "!";
            case COLON_COLON -> "::";
            case PERCENT -> "%";
            case QUESTION_MARK -> "?";
            case COLON -> ":";
            case DOLLAR_UNDER_SCORE -> "$_";
            case DOLLAR -> "$";
            case SKINNY_ARROW -> "->";
            case ANGLE_ANGLE -> ">>";
            case COMMA -> ",";
            case LEFT_BRACKET -> "[";
            case RIGHT_BRACKET -> "]";
            case LEFT_BRACE -> "{";
            case RIGHT_BRACE -> "}";
            case PLUS_EQUALS -> "+=";
            case MINUS_EQUALS -> "-=";
            case START_EQUALS -> "*=";
            case SLASH_EQUALS -> "/=";
            case CARET_EQUALS -> "^=";
            case PLUS_PLUS -> "++";
            case MINUS_MINUS -> "--";
            case DOT -> ".";
            case HASHTAG -> "#";
            case LEFT_ARROW -> "<-";
            case BACK_SLASH -> "\\";
            case DOT_DOT -> "..";
            default -> "";
        };
    }

    public ISyntaxPosition getSyntaxPosition() {
        return new MutableSyntaxImpl(startPosition, endPosition);
    }

    public TokenType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public Position getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Position startPosition) {
        this.startPosition = startPosition;
    }

    public Position getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Position endPosition) {
        this.endPosition = endPosition;
    }
}
