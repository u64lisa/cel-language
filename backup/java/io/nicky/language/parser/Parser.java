package language.frontend.parser;

import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.cases.ElseCase;
import language.frontend.parser.results.ParseResult;
import language.frontend.parser.units.Argument;
import language.frontend.parser.units.NamingConvention;
import language.frontend.parser.units.TokenMatcher;
import language.utils.Pair;
import parser_rewrite.RewriteParser;

import java.util.Arrays;
import java.util.List;

public abstract class Parser {

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
        return new RewriteParser(tokens);
    }

    public abstract ParseResult<Node> parse();

    public abstract ParseResult<Node> statements(List<TokenMatcher> tks);

    public abstract ParseResult<Node> parseStatement();

    public abstract ParseResult<Token> parseVar(boolean screaming);

    public abstract ParseResult<Node> chainExpr();

    public abstract ParseResult<Token> parseTypeToken();

    public abstract ParseResult<Node> parseExpression();

    public abstract ParseResult<Node> parseFactor();

    public abstract ParseResult<Token> parseIdentifier();

    public abstract ParseResult<Token> parseIdentifier(String name, NamingConvention convention);

    public abstract ParseResult<Node> parseExpressionCall();

    public abstract ParseResult<Node> parseBitShiftExpression();

    public abstract ParseResult<Node> parseBitWiseExpression();

    public abstract ParseResult<Node> parseComplexExpression();

    public abstract ParseResult<Node> byteExpr();

    public abstract ParseResult<Node> atom();

    public abstract ParseResult<Node> parseReferenceExpression();

    public abstract ParseResult<Node> formatStringExpr();

    public abstract ParseResult<Node> parseThrowExpression();

    public abstract ParseResult<Node> parseStructureDefinition();

    public abstract ParseResult<Node> parseEnumExpression();

    public abstract ParseResult<Node> parseSwitchExpression();

    public abstract ParseResult<Void> expectSemicolon();

    public abstract ParseResult<Node> patternExpr(Node expr);

    public abstract ParseResult<Node> parseMatchingExpression();

    public abstract ParseResult<Node> call();

    public abstract ParseResult<Node> index();

    public abstract ParseResult<Node> pow();

    public abstract ParseResult<Node> refOp();

    public abstract ParseResult<Node> refSugars();

    public abstract ParseResult<Node> parseTerm();

    public abstract ParseResult<Node> parseAttribute();

    public abstract ParseResult<Node> parseComparisonExpression();

    public abstract ParseResult<Node> parseBitExpression();

    public abstract ParseResult<Node> binOp(ParseExecutable left, List<TokenType> ops);

    public abstract ParseResult<Node> binOp(ParseExecutable left, List<TokenType> ops, ParseExecutable right);

    public abstract ParseResult<Argument> parseArguments();

    public abstract void endLine(int offset);

    public abstract ParseResult<Node> parseBlock();

    public abstract ParseResult<Node> parseBlock(boolean vLine);

    public abstract ParseResult<Node> parseIfExpression();

    public abstract ParseResult<Pair<List<Case>, ElseCase>> parseIfCases(String caseKeyword, boolean parenthesis);

    public abstract ParseResult<Pair<List<Case>, ElseCase>> parseElseIfStatement(boolean parenthesis);

    public abstract ParseResult<Pair<List<Case>, ElseCase>> parseElifExpression(boolean parenthesis);

    public abstract ParseResult<ElseCase> elseExpr();

    public abstract ParseResult<Node> kv();

    public abstract ParseResult<Node> parseQueryExpression();

    public abstract ParseResult<Node> parseForLoop();

    public abstract ParseResult<Node> getClosing();

    public abstract ParseResult<Node> parseWhileLoop();

    public abstract ParseResult<Node> parseDoExpression();

    public abstract ParseResult<Node> parseWhileExpression();

    public abstract ParseResult<Node> parseListExpression();

    public abstract ParseResult<Node> parseDictionaryExpression();

    public abstract ParseResult<Boolean> isCatcher();

    public abstract ParseResult<Node> parseFunctionDefinition();

    public abstract ParseResult<List<String>> parseStaticReturn();

    public abstract ParseResult<Node> parseClassDefinition();
}
