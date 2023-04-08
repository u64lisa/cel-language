package language.frontend.parser;

import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.results.ParseResult;

import java.util.Arrays;
import java.util.List;

public abstract class Parser {

    private static final boolean DEBUG = true;

    public static final List<TokenType> TYPE_TOKENS = Arrays.asList(
            TokenType.IDENTIFIER,
            TokenType.KEYWORD,

            TokenType.FLOAT,
            TokenType.DOUBLE,
            TokenType.LONG,
            TokenType.BYTE,
            TokenType.SHORT,
            TokenType.INTEGER,

            TokenType.LEFT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.LEFT_BRACKET,
            TokenType.RIGHT_BRACKET,
            TokenType.RIGHT_ANGLE,
            TokenType.LEFT_ANGLE,
            TokenType.QUESTION_MARK
    );

    public static Parser getParser(final List<Token> tokens) {
        return DEBUG ? new RewriteParser(tokens) : new CelParser(tokens);
    }

    public abstract ParseResult<Node> parse();

}
