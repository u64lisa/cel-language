package language.frontend.lexer.token;

import java.util.HashMap;
import java.util.Map;

public class Operators {
    public static final Map<String, TokenType> GLOBAL_OPERATION_TOKENS = new HashMap<>() {{
        put("[", TokenType.LEFT_BRACKET);
        put("\\", TokenType.BACK_SLASH);
        put("..", TokenType.DOT_DOT);
        put("~~", TokenType.TILDE_TILDE);
        put("~>", TokenType.RIGHT_TILDE_ARROW);
        put("<~", TokenType.LEFT_TILDE_ARROW);
        put("@", TokenType.AT);
        put("~&", TokenType.TILDE_AMPERSAND);
        put("~|", TokenType.TILDE_PIPE);
        put("~^", TokenType.TILDE_CARET);
        put("~", TokenType.TILDE);
        put("<-", TokenType.LEFT_ARROW);
        put("::", TokenType.COLON_COLON);
        put("%", TokenType.PERCENT);
        put("#", TokenType.HASHTAG);
        put("]", TokenType.RIGHT_BRACKET);
        put(",", TokenType.COMMA);
        put("+", TokenType.PLUS);
        put("++", TokenType.PLUS_PLUS);
        put("--", TokenType.MINUS_MINUS);
        put(">>", TokenType.ANGLE_ANGLE);
        put(":", TokenType.COLON);
        put("$", TokenType.DOLLAR);
        put("$_", TokenType.DOLLAR_UNDER_SCORE);
        put("?", TokenType.QUESTION_MARK);
        put("-", TokenType.MINUS);
        put("*", TokenType.STAR);
        put("/", TokenType.SLASH);
        put("(", TokenType.LEFT_PAREN);
        put(")", TokenType.RIGHT_PAREN);
        put("^", TokenType.CARET);
        put("=>", TokenType.FAT_ARROW);
        put("&", TokenType.AMPERSAND);
        put("|", TokenType.PIPE);
        put("->", TokenType.SKINNY_ARROW);
        put(";", TokenType.NEW_LINE);
        put("{", TokenType.LEFT_BRACE);
        put("}", TokenType.RIGHT_BRACE);
        put("^=", TokenType.CARET_EQUALS);
        put("*=", TokenType.START_EQUALS);
        put("/=", TokenType.SLASH_EQUALS);
        put("+=", TokenType.PLUS_EQUALS);
        put("-=", TokenType.MINUS_EQUALS);
        put(".", TokenType.DOT);
    }};
}
