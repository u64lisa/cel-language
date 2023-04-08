package language.frontend.lexer.token;

public enum TokenType {
    IGNORED,

    TYPE,
    INVISIBLE_NEW_LINE,
    LEFT_TILDE_ARROW,
    TILDE_TILDE,
    RIGHT_TILDE_ARROW,
    EQUAL,
    TILDE_AMPERSAND,
    TILDE_PIPE,
    TILDE_CARET,
    TILDE,
    AT,

    INTEGER,
    FLOAT,
    LONG,
    BYTE,
    DOUBLE,
    SHORT,

    STRING,
    BOOLEAN,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    LEFT_PAREN,
    RIGHT_PAREN,
    EOF,
    NEW_LINE,
    CARET,
    IDENTIFIER,
    KEYWORD,
    FAT_ARROW,
    EQUAL_EQUAL,
    BANG_EQUAL,
    LEFT_ANGLE,
    RIGHT_ANGLE,
    LESS_EQUALS,
    GREATER_EQUALS,
    AMPERSAND,
    PIPE,
    BANG,
    COLON_COLON,
    PERCENT,
    QUESTION_MARK,
    COLON,
    DOLLAR_UNDER_SCORE,
    DOLLAR,
    SKINNY_ARROW,
    ANGLE_ANGLE,
    COMMA,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    LEFT_BRACE,
    RIGHT_BRACE,
    PLUS_EQUALS,
    MINUS_EQUALS,
    START_EQUALS,
    SLASH_EQUALS,
    CARET_EQUALS,
    PLUS_PLUS,
    MINUS_MINUS,
    DOT,
    HASHTAG,
    LEFT_ARROW,
    BACK_SLASH,
    DOT_DOT, APOSTROPH;

    public static final TokenType[] values = TokenType.values();

    public String toString() {
        return switch (this) {
            case TYPE -> "type";
            case INVISIBLE_NEW_LINE -> "invisible newline";
            case LEFT_TILDE_ARROW -> "<~";
            case TILDE_TILDE -> "~~";
            case RIGHT_TILDE_ARROW -> "~>";
            case EQUAL -> "=";
            case TILDE_AMPERSAND -> "~&";
            case TILDE_PIPE -> "~|";
            case TILDE_CARET -> "~^";
            case TILDE -> "~";
            case AT -> "@";
            case APOSTROPH -> "'";

            case INTEGER -> "i32";
            case FLOAT -> "f64";
            case BYTE -> "i8";
            case LONG -> "l64";
            case DOUBLE -> "i64";
            case SHORT -> "i16";
            case STRING -> "string";
            case BOOLEAN -> "boolean";

            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case LEFT_PAREN -> "(";
            case RIGHT_PAREN -> ")";
            case EOF -> "end of file";
            case NEW_LINE -> "newline";
            case CARET -> "^";
            case IDENTIFIER -> "identifier";
            case KEYWORD -> "keyword";
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
            case IGNORED -> "skip this token no one needs it";
        };
    }

    public static TokenType valueOfName(final String type) {
        for (TokenType value : values()) {
            if (value.name().equals(type))
                return value;
        }
        return IGNORED;
    }
}