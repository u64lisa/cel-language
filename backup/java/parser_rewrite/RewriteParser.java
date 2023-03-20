package parser_rewrite;

import language.frontend.lexer.Lexer;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import dtool.logger.errors.LanguageException;
import language.frontend.parser.ParseExecutable;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.cases.ElseCase;
import language.frontend.parser.nodes.definitions.*;
import language.frontend.parser.nodes.expressions.*;
import language.frontend.parser.nodes.operations.BinOpNode;
import language.frontend.parser.nodes.operations.UnaryOpNode;
import language.frontend.parser.nodes.values.*;
import language.frontend.parser.nodes.variables.AttributeAccessNode;
import language.frontend.parser.nodes.variables.TypeDefinitionNode;
import language.frontend.parser.nodes.variables.VarAccessNode;
import language.frontend.parser.results.ParseResult;
import language.frontend.parser.units.*;
import language.utils.Pair;
import language.utils.WrappedCast;

import java.util.*;

public class RewriteParser extends Parser {

    final List<TokenType> binRefOps = Arrays.asList(
            TokenType.PLUS_EQUALS, TokenType.MINUS_EQUALS,
            TokenType.START_EQUALS, TokenType.SLASH_EQUALS,
            TokenType.CARET_EQUALS
    );

    final List<TokenType> unRefOps = Arrays.asList(
            TokenType.PLUS_PLUS, TokenType.MINUS_MINUS
    );

    private final List<String> declarationKeywords = Arrays.asList(
            "static", "private", "public"
    );
    private final List<String> classKeywords = Arrays.asList(
            "class", "object"
    );
    private final List<String> constructorWords = Arrays.asList(
            "constructor", "call", "init"
    );

    private Token currentToken;
    private final List<Token> tokens;
    private int tokenIndex = -1;
    private int tokenCount;

    public RewriteParser(List<Token> tokens) {
        this.tokens = tokens;
        this.tokenCount = tokens.size();
        this.advance();
    }

    @Override
    public ParseResult<Node> parse() {
        ParseResult<Node> result = statements(Collections.singletonList(new TokenMatcher(TokenType.EOF, null)));

        if (result.getLanguageError() == null && !currentToken.getType().equals(TokenType.EOF)) {
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '+', '-', '*', '^', or '/'"));
        }
        return result;
    }

    @Override
    public ParseResult<Node> statements(List<TokenMatcher> tks) {
        ParseResult<Node> result = new ParseResult<>();
        List<Node> statements = new ArrayList<>();

        int newlineCount;
        while (currentToken.getType().equals(TokenType.NEW_LINE) ||
                currentToken.getType().equals(TokenType.INVISIBLE_NEW_LINE)) {
            result.registerAdvancement();
            advance();
        }

        Node statement = result.register(this.parseStatement());
        if (result.getLanguageError() != null) return result;
        statements.add(statement);

        boolean moreStatements = true;

        do {
            newlineCount = 0;
            while (currentToken.getType().equals(TokenType.NEW_LINE) ||
                    currentToken.getType().equals(TokenType.INVISIBLE_NEW_LINE)) {
                result.registerAdvancement();
                advance();
                newlineCount++;
            }

            if (newlineCount == 0) {
                moreStatements = false;
            }

            for (TokenMatcher matcher : tks) {
                if (currentToken.getType() == matcher.type() && (matcher.value() == null ||
                        matcher.value().equals(currentToken.getValue()))) {
                    moreStatements = false;
                    break;
                }
            }

            if (!moreStatements)
                break;

            statement = result.tryRegister(this.parseStatement());
            if (statement == null) {
                reverse(result.getToReverseCount());
                moreStatements = false;
                continue;
            }
            statements.add(statement);
        } while (moreStatements);

        reverse(1);

        if (!currentToken.getType().equals(TokenType.NEW_LINE) &&
                !currentToken.getType().equals(TokenType.INVISIBLE_NEW_LINE)) {
            Node prevStatement = statements.get(statements.size() - 1);
            statements.set(statements.size() - 1, new ReturnNode(prevStatement, prevStatement.getStartPosition(), prevStatement.getEndPosition()));
        }
        advance();
        return result.success(new BodyNode(statements));
    }

    @Override
    public ParseResult<Node> parseStatement() {
        ParseResult<Node> result = new ParseResult<>();
        Position start = currentToken.getStartPosition().copy();

        if (currentToken.matches(TokenType.KEYWORD, "return")) {
            result.registerAdvancement();
            advance();
            Node expr = null;

            if (currentToken.getType() != TokenType.NEW_LINE && currentToken.getType() != TokenType.INVISIBLE_NEW_LINE) {
                expr = result.register(this.parseExpression());
                if (result.getLanguageError() != null) return result;
            }

            return result.success(new ReturnNode(expr, start, currentToken.getEndPosition().copy()));
        } else if (currentToken.matches(TokenType.KEYWORD, "continue")) {
            result.registerAdvancement();
            advance();
            return result.success(new ContinueNode(start, currentToken.getEndPosition().copy()));
        } else if (currentToken.matches(TokenType.KEYWORD, "break")) {
            result.registerAdvancement();
            advance();
            return result.success(new BreakNode(start, currentToken.getEndPosition().copy()));
        } else if (currentToken.matches(TokenType.KEYWORD, "pass")) {
            result.registerAdvancement();
            advance();
            return result.success(new PassNode(start, currentToken.getEndPosition().copy()));
        } else if (currentToken.getType() == TokenType.LEFT_BRACE) {
            Node statements = result.register(parseBlock());
            if (result.getLanguageError() != null) return result;
            return result.success(new ScopeNode(null, statements));
        } else if (currentToken.getType() == TokenType.KEYWORD) {
            switch (currentToken.getValue().toString()) {
                case "for" -> {
                    Node forExpr = result.register(this.parseForLoop());
                    if (result.getLanguageError() != null) return result;
                    return result.success(forExpr);
                }
                case "break" -> {
                    result.registerAdvancement();
                    advance();
                    return result.success(new BreakNode(start, currentToken.getEndPosition().copy()));
                }
                case "continue" -> {
                    result.registerAdvancement();
                    advance();
                    return result.success(new ContinueNode(start, currentToken.getEndPosition().copy()));
                }
                case "return" -> {
                    result.registerAdvancement();
                    advance();
                    Node expr = null;
                    if (currentToken.getType() != TokenType.NEW_LINE && currentToken.getType() != TokenType.INVISIBLE_NEW_LINE) {
                        expr = result.register(this.parseExpression());
                        if (result.getLanguageError() != null) return result;
                    }
                    return result.success(new ReturnNode(expr, start, currentToken.getEndPosition().copy()));
                }
                case "pass" -> {
                    result.registerAdvancement();
                    advance();
                    return result.success(new PassNode(start, currentToken.getEndPosition().copy()));
                }
                case "assert" -> {
                    result.registerAdvancement();
                    advance();
                    Node condition = result.register(parseExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(new AssertNode(condition));
                }
                case "typedef" -> {
                    advance();

                    final Token known = currentToken;
                    advance(); // known type
                    advance(); // : symbol
                    final Token newName = currentToken;

                    result.registerAdvancement(); // new name
                    advance(); // ; symbol

                    // todo register typedef for parsing
                    return result.success(new TypeDefinitionNode(known, newName));
                }
                case "destructor" -> {
                    Token varTok = result.register(parseIdentifier());
                    if (result.getLanguageError() != null) return result;
                    result.registerAdvancement();
                    advance();
                    return result.success(new DropNode(varTok));
                }
                case "throw" -> {
                    Node throwNode = result.register(this.parseThrowExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(throwNode);
                }
                case "class", "object", "recipe" -> {
                    Node classDef = result.register(this.parseClassDefinition());
                    if (result.getLanguageError() != null) return result;
                    return result.success(classDef);
                }
                case "if" -> {
                    Node ifExpr = result.register(this.parseIfExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(ifExpr);
                }
                case "enum" -> {
                    Node enumExpr = result.register(this.parseEnumExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(enumExpr);
                }
                case "switch" -> {
                    Node switchExpr = result.register(this.parseSwitchExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(switchExpr);
                }
                case "structure" -> {
                    Node structDef = result.register(this.parseStructureDefinition());
                    if (result.getLanguageError() != null) return result;
                    return result.success(structDef);
                }
                case "loop", "while" -> {
                    Node whileExpr = result.register(this.parseWhileExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(whileExpr);
                }
                case "do" -> {
                    Node doExpr = result.register(this.parseDoExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(doExpr);
                }
                case "import" -> {
                    advance();
                    result.registerAdvancement();
                    Token importName = currentToken;
                    if (importName.getType() != TokenType.STRING && importName.getType() != TokenType.IDENTIFIER)
                        return result.failure(LanguageException.invalidSyntax(importName.getStartPosition().copy(),
                                importName.getEndPosition().copy(), "Expected module name"));

                    advance();
                    result.registerAdvancement();
                    if (currentToken.matches(TokenType.KEYWORD, "as")) {
                        Token identifier = result.register(parseIdentifier("Module name", NamingConvention.SnakeCase));
                        if (result.getLanguageError() != null) return result;
                        result.registerAdvancement();
                        advance();
                        return result.success(new ImportNode(importName, identifier));
                    }
                    return result.success(new ImportNode(importName));
                }

                case "package" -> {
                    advance();
                    result.registerAdvancement();

                    if (currentToken.getType() != TokenType.STRING && currentToken.getType() != TokenType.IDENTIFIER)
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                                currentToken.getEndPosition().copy(), "Expected package name"));

                    final Token nameToken = currentToken;


                    advance();
                    result.registerAdvancement();

                    return result.success(new PackageNode(nameToken));

                }

                case "extend" -> {
                    advance();
                    result.registerAdvancement();

                    if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                                currentToken.getEndPosition().copy(), "Expected module name"));

                    advance();
                    result.registerAdvancement();
                    return result.success(new ExtendNode(currentToken));
                }
            }
        } else if (currentToken.getType().equals(TokenType.HASHTAG)) {
            Node useExpr = result.register(this.parseExpressionCall());
            if (result.getLanguageError() != null) return result;
            return result.success(useExpr);
        }

        Node expr = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;
        return result.success(expr);
    }

    public ParseResult<Token> parseVar() {
        return parseVar(false);
    }

    @Override
    public ParseResult<Token> parseVar(boolean screaming) {
        ParseResult<Token> result = new ParseResult<>();

        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected identifier"));

        Token name = currentToken;
        result.registerAdvancement();
        advance();
        return result.success(name);
    }

    @Override
    public ParseResult<Node> chainExpr() {
        return binOp(this::parseComparisonExpression, Collections.singletonList(TokenType.COLON));
    }

    @Override
    public ParseResult<Token> parseTypeToken() {
        List<String> type = new ArrayList<>();
        Stack<Token> parens = new Stack<>();
        ParseResult<Token> result = new ParseResult<>();
        Position start = currentToken.getStartPosition();
        Position end = start;

        if (!TYPE_TOKENS.contains(currentToken.getType()))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected type"));

        ParseType:
        while (!parens.isEmpty() || TYPE_TOKENS.contains(currentToken.getType())) {
            if (currentToken == null)
                return result.failure(LanguageException.invalidSyntax(start, end, "Unmatched parenthesis"));
            switch (currentToken.getType()) {
                case LEFT_ANGLE, LEFT_BRACE, LEFT_BRACKET, LEFT_PAREN -> parens.push(currentToken);
                case RIGHT_ANGLE -> {
                    if (parens.isEmpty()) break ParseType;
                    if (!parens.peek().getType().equals(TokenType.LEFT_ANGLE))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Unmatched '>'"));
                    parens.pop();
                }
                case ANGLE_ANGLE -> {
                    if (parens.isEmpty()) break ParseType;
                    if (!parens.peek().getType().equals(TokenType.LEFT_ANGLE))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Unmatched '>'"));
                    parens.pop();
                    if (!parens.peek().getType().equals(TokenType.LEFT_ANGLE))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Unmatched '>'"));
                    parens.pop();
                }
                case RIGHT_PAREN -> {
                    if (parens.isEmpty()) break ParseType;
                    if (!parens.peek().getType().equals(TokenType.LEFT_PAREN))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Unmatched ')'"));
                    parens.pop();
                }
                case RIGHT_BRACKET -> {
                    if (parens.isEmpty()) break ParseType;
                    if (!parens.peek().getType().equals(TokenType.LEFT_BRACKET))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Unmatched ']'"));
                    parens.pop();
                }
                case RIGHT_BRACE -> {
                    if (parens.isEmpty()) break ParseType;
                    if (!parens.peek().getType().equals(TokenType.LEFT_BRACE))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Unmatched '}'"));
                    parens.pop();
                }
            }
            if (currentToken.getType().equals(TokenType.ANGLE_ANGLE)) {
                type.add(">");
                type.add(">");
            } else {
                type.add(currentToken.asString());
            }
            end = currentToken.getEndPosition();
            result.registerAdvancement();
            advance();
        }

        return result.success(new Token(TokenType.TYPE, type, start, end));
    }

    @Override
    public ParseResult<Node> parseExpression() {
        ParseResult<Node> result = new ParseResult<>();
        List<String> type = Collections.singletonList("any");
        if (currentToken.matches(TokenType.KEYWORD, "attribute")) {
            result.registerAdvancement();
            advance();
            Token name = result.register(parseVar());
            if (result.getLanguageError() != null) return result;

            if (currentToken.getType().equals(TokenType.FAT_ARROW))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Should be '='"));
            if (!currentToken.getType().equals(TokenType.EQUAL))
                return result.success(new AttributeAccessNode(name));

            result.registerAdvancement();
            advance();

            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            return result.success(new AttributeAssignNode(name, expr));
        } else if (currentToken.getType() == TokenType.KEYWORD &&
                ("constant".equals(currentToken.getValue().toString()) || "var".equals(currentToken.getValue().toString()))) {
            boolean locked = "constant".equalsIgnoreCase(currentToken.getValue().toString());

            result.registerAdvancement();
            advance();
            if (currentToken.getType() == TokenType.LEFT_BRACE) {
                // Destructors
                List<Token> destructs = new ArrayList<>();
                boolean glob = false;

                result.registerAdvancement();
                advance();

                // Glob
                if (currentToken.getType() == TokenType.STAR) {
                    result.registerAdvancement();
                    advance();
                    glob = true;
                } else do {
                    if (currentToken.getType() != TokenType.IDENTIFIER)
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected identifier"));

                    destructs.add(currentToken);
                    result.registerAdvancement();
                    advance();
                } while (currentToken.getType() != TokenType.RIGHT_BRACE);

                if (currentToken.getType() != TokenType.RIGHT_BRACE)
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '}'"));
                result.registerAdvancement();
                advance();

                if (currentToken.getType() != TokenType.FAT_ARROW)
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "222 Expected '=>'"));
                result.registerAdvancement();
                advance();

                Node destructed = result.register(parseStatement());
                if (result.getLanguageError() != null) return result;

                if (glob) return result.success(new DestructNode(destructed));
                return result.success(new DestructNode(destructed, destructs));
            }
            Token name = result.register(parseVar(locked));
            if (result.getLanguageError() != null) return result;

            Integer min = null;
            Integer max = null;

            if (currentToken.getType() == TokenType.COMMA) {
                Node nll = new NullNode(new Token(TokenType.IDENTIFIER, "null", currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy()));
                List<Node> varNames = new ArrayList<>(Collections.singletonList(new VarAssignNode(name, nll).setType(type)));
                do {
                    name = result.register(parseIdentifier("Variable name", NamingConvention.CamelCase));
                    if (result.getLanguageError() != null) return result;
                    varNames.add(new VarAssignNode(name, nll).setType(type));
                    result.registerAdvancement();
                    advance();
                } while (currentToken.getType() == TokenType.COMMA);
                return result.success(new BodyNode(varNames));
            }

            if (currentToken.getType() == TokenType.LEFT_BRACKET) {
                result.registerAdvancement();
                advance();
                boolean neg = false;
                if (currentToken.getType() == TokenType.MINUS) {
                    neg = true;
                    result.registerAdvancement();
                    advance();
                }
                if (currentToken.getType() != TokenType.INTEGER)
                    return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected integer"));
                min = 0;
                max = ((Double) currentToken.getValue()).intValue() * (neg ? -1 : 1);
                result.registerAdvancement();
                advance();
                if (currentToken.getType() == TokenType.PIPE) {
                    result.registerAdvancement();
                    advance();
                    neg = false;
                    if (currentToken.getType() == TokenType.MINUS) {
                        neg = true;
                        result.registerAdvancement();
                        advance();
                    }
                    if (currentToken.getType() != TokenType.INTEGER)
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected integer"));
                    min = max;
                    max = ((Double) currentToken.getValue()).intValue() * (neg ? -1 : 1);
                    result.registerAdvancement();
                    advance();
                }
                if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                    return result.failure(LanguageException.expected(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected ']'"));
                result.registerAdvancement();
                advance();
            }

            if (currentToken.getType().equals(TokenType.COLON)) {
                advance();
                result.registerAdvancement();
                Token typeTok = result.register(parseTypeToken());
                if (result.getLanguageError() != null) return result;
                type = WrappedCast.cast(typeTok.getValue());
            }

            if (currentToken.getType().equals(TokenType.FAT_ARROW))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Should be '='"));
            if (!currentToken.getType().equals(TokenType.EQUAL))
                return result.success(new VarAssignNode(name, new NullNode(new Token(TokenType.IDENTIFIER, "null", currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy()))).setType(type));

            result.registerAdvancement();
            advance();
            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            return result.success(new VarAssignNode(name, expr, locked).setType(type).setRange(min, max));
        } else if (currentToken.matches(TokenType.KEYWORD, "let")) {
            Token identifier = result.register(parseIdentifier("Variable name", NamingConvention.CamelCase));
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            advance();

            if (currentToken.getType() != TokenType.EQUAL)
                return result.failure(LanguageException.expected(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected '='"));
            result.registerAdvancement();

            advance();

            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;

            return result.success(new LetNode(identifier, expr));
        } else if (currentToken.matches(TokenType.KEYWORD, "cal")) {
            result.registerAdvancement();
            advance();
            Token name = result.register(parseVar());
            if (result.getLanguageError() != null) return result;

            if (!currentToken.getType().equals(TokenType.SKINNY_ARROW))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected weak assignment arrow (->)"));

            result.registerAdvancement();
            advance();
            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            return result.success(new MacroAssignNode(name, expr));
        } else if (currentToken.getType().equals(TokenType.IDENTIFIER)) {
            Token variable = currentToken;
            advance();
            result.registerAdvancement();
            if (Arrays.asList(TokenType.CARET_EQUALS, TokenType.PLUS_EQUALS, TokenType.START_EQUALS,
                    TokenType.SLASH_EQUALS, TokenType.MINUS_EQUALS).contains(currentToken.getType())) {
                Token operationToken = currentToken;
                advance();
                result.registerAdvancement();
                Node value = result.register(this.parseExpression());
                if (result.getLanguageError() != null) return result;
                TokenType op = switch (operationToken.getType()) {
                    case CARET_EQUALS -> TokenType.CARET;
                    case PLUS_EQUALS -> TokenType.PLUS;
                    case START_EQUALS -> TokenType.STAR;
                    case SLASH_EQUALS -> TokenType.SLASH;
                    case MINUS_EQUALS -> TokenType.MINUS;
                    default -> null;
                };

                return result.success(new VarAssignNode(variable, new BinOpNode(
                        new VarAccessNode(variable), op, value), false)
                        .setDefining(false));
            }
            if (currentToken.getType().equals(TokenType.PLUS_PLUS) || currentToken.getType().equals(TokenType.MINUS_MINUS)) {
                Token operationToken = currentToken;
                result.registerAdvancement();
                advance();
                return result.success(new VarAssignNode(variable, new UnaryOpNode(operationToken.getType(),
                        new VarAccessNode(variable)), false).setDefining(false));
            }
            if (currentToken.getType().equals(TokenType.FAT_ARROW)) {
                result.registerAdvancement();
                advance();
                Node value = result.register(parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new VarAssignNode(variable, value, false, 1));
            }
            reverse(1);
        }
        // |Type cast| expr
        if (currentToken.getType() == TokenType.PIPE) {
            result.registerAdvancement();
            advance();
            Token cast = result.register(parseTypeToken());
            if (result.getLanguageError() != null) return result;
            if (currentToken.getType() != TokenType.PIPE)
                return result.failure(LanguageException
                        .expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '|'"));
            result.registerAdvancement();
            advance();
            Node expr = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(new CastNode(expr, cast));
        }
        Node node = result.register(binOp(this::parseBitExpression, Collections.singletonList(TokenType.DOT), this::call));

        if (result.getLanguageError() != null) return result;

        return result.success(node);
    }

    @Override
    public ParseResult<Node> parseFactor() {
        ParseResult<Node> result = new ParseResult<>();
        Token tok = currentToken;

        if (Arrays.asList(TokenType.PLUS, TokenType.MINUS, TokenType.PLUS_PLUS, TokenType.MINUS_MINUS).contains(tok.getType())) {
            result.registerAdvancement();
            advance();
            Node factor = result.register(this.parseFactor());
            if (result.getLanguageError() != null) return result;
            return result.success(new UnaryOpNode(tok.getType(), factor));
        }
        return pow();
    }

    @Override
    public ParseResult<Token> parseIdentifier() {
        return parseIdentifier("Identifier", NamingConvention.None);
    }

    @Override
    public ParseResult<Token> parseIdentifier(String name, NamingConvention convention) {
        ParseResult<Token> result = new ParseResult<>();
        advance();
        result.registerAdvancement();
        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), String.format("Expected %s", name.toLowerCase())));


        return result.success(currentToken);
    }

    @Override
    public ParseResult<Node> parseExpressionCall() {
        ParseResult<Node> result = new ParseResult<>();
        Token useToken = result.register(parseIdentifier());

        if (result.getLanguageError() != null) return result;
        advance();
        result.registerAdvancement();
        List<Token> args = new ArrayList<>();
        while (currentToken.getType().equals(TokenType.IDENTIFIER)) {
            args.add(currentToken);
            result.registerAdvancement();
            advance();
        }
        return result.success(new UseNode(useToken, args));
    }

    @Override
    public ParseResult<Node> parseBitShiftExpression() {
        return binOp(this::parseBitWiseExpression, Arrays.asList(TokenType.LEFT_TILDE_ARROW, TokenType.TILDE_TILDE, TokenType.RIGHT_TILDE_ARROW), this::parseExpression);
    }

    @Override
    public ParseResult<Node> parseBitWiseExpression() {
        return binOp(this::parseComplexExpression, Arrays.asList(TokenType.TILDE_AMPERSAND, TokenType.TILDE_PIPE, TokenType.TILDE_CARET), this::parseExpression);
    }

    @Override
    public ParseResult<Node> parseComplexExpression() {
        ParseResult<Node> result = new ParseResult<>();
        if (currentToken.getType() == TokenType.TILDE || currentToken.getType() == TokenType.DOLLAR) {
            Token opToken = currentToken;
            result.registerAdvancement();
            advance();

            Node expr = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;

            return result.success(new UnaryOpNode(opToken.getType(), expr));
        }
        return byteExpr();
    }

    @Override
    public ParseResult<Node> byteExpr() {
        ParseResult<Node> result = new ParseResult<>();
        boolean toBytes = currentToken.getType() == TokenType.AT;
        if (toBytes) {
            result.registerAdvancement();
            advance();
        }

        Node expr = result.register(this.chainExpr());
        if (result.getLanguageError() != null) return result;

        if (toBytes) return result.success(new BytesNode(expr));
        return result.success(expr);
    }

    @Override
    public ParseResult<Node> atom() {
        ParseResult<Node> result = new ParseResult<>();
        Token tok = currentToken;

        // If it's a statement, then { means scope not dictionary
        // However, the scope keyword means it's a scope anywhere
        if (tok.getType() == TokenType.KEYWORD) switch (tok.getValue().toString()) {
            case "attribute" -> {
                advance();
                result.registerAdvancement();
                Token name = currentToken;
                if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                    return result.failure(LanguageException.invalidSyntax(name.getStartPosition().copy(), name.getEndPosition().copy(), "Expected identifier"));
                advance();
                result.registerAdvancement();
                return result.success(new AttributeAccessNode(name));
            }
            case "scope" -> {
                result.registerAdvancement();
                advance();
                String name = null;
                if (currentToken.getType() == TokenType.LEFT_BRACKET) {
                    Token n = result.register(parseIdentifier("Scope", NamingConvention.SnakeCase));
                    if (result.getLanguageError() != null) return result;

                    name = n.getValue().toString();

                    result.registerAdvancement();
                    advance();
                    if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                        return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected ']'"));
                    result.registerAdvancement();
                    advance();
                }
                Node statements = result.register(parseBlock());
                if (result.getLanguageError() != null) return result;
                return result.success(new ScopeNode(name, statements));
            }
            case "match" -> {
                Node matchExpr = result.register(this.parseMatchingExpression());
                if (result.getLanguageError() != null) return result;
                return result.success(matchExpr);
            }
            case "function" -> {
                Node funcDef = result.register(this.parseFunctionDefinition());
                if (result.getLanguageError() != null) return result;
                return result.success(funcDef);
            }
            case "null" -> {
                result.registerAdvancement();
                advance();
                return result.success(new NullNode(tok));
            }
        }
        else if (currentToken.getType() == TokenType.SLASH) {
            // Decorator
            // /decorator/ fn abc { xyz; }
            advance();
            result.registerAdvancement();
            Node decorator = result.register(parseFactor());
            if (result.getLanguageError() != null) return result;
            if (currentToken.getType() != TokenType.SLASH)
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Expected closing slash"));
            result.registerAdvancement();
            advance();
            Node fn = result.register(parseStatement());
            if (result.getLanguageError() != null) return result;
            Token name;
            if (fn.getNodeType() == NodeType.FUNCTION_DEFINITION) name = ((FunctionDeclareNode) fn).name;
            else if (fn.getNodeType() == NodeType.CLASS_DEFINITION) name = ((ClassDefNode) fn).className;
            else if (fn.getNodeType() == NodeType.DECORATOR) name = ((DecoratorNode) fn).name;
            else
                return result.failure(LanguageException.invalidSyntax(fn.getStartPosition().copy(), fn.getEndPosition().copy(), "Object is not deco-ratable"));
            return result.success(new DecoratorNode(decorator, fn, name));
        } else if (Arrays.asList(TokenType.INTEGER, TokenType.FLOAT, TokenType.DOUBLE,
                TokenType.LONG, TokenType.SHORT, TokenType.BYTE).contains(tok.getType())) {
            result.registerAdvancement();
            advance();
            if (currentToken.getType() == TokenType.IDENTIFIER) {
                if (currentToken.getValue().toString().startsWith("x") && tok.getValue().equals(0.0) && tok.getType().equals(TokenType.INTEGER)) {
                    try {
                        Token hexTk = currentToken;
                        result.registerAdvancement();
                        advance();
                        int hexForm = Integer.parseInt(hexTk.getValue().toString().substring(1), 16);
                        return result.success(new NumberNode(hexForm, hexTk.getStartPosition(), hexTk.getEndPosition()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                Node identifier = new VarAccessNode(currentToken);
                result.registerAdvancement();
                advance();
                return result.success(new BinOpNode(new NumberNode(tok), TokenType.STAR, identifier));
            } else if (currentToken.getType() == TokenType.LEFT_PAREN) {
                // 3(1 + 2) = 3 * (1 + 2)
                Node node = new NumberNode(tok);
                while (currentToken.getType() == TokenType.LEFT_PAREN) {
                    result.registerAdvancement();
                    advance();
                    Node expr = result.register(parseExpression());
                    if (result.getLanguageError() != null) return result;
                    if (currentToken.getType() != TokenType.RIGHT_PAREN)
                        return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                                currentToken.getEndPosition().copy(), "Expected ')' to close expression"));
                    result.registerAdvancement();
                    advance();
                    node = new BinOpNode(node, TokenType.STAR, expr);
                }
                return result.success(node);
            }
            return result.success(new NumberNode(tok));
        } else if (tok.getType().equals(TokenType.STRING)) {
            final Pair<String, Boolean> element = WrappedCast.cast(tok.getValue());
            if (element.getLast()) {
                Node val = result.register(formatStringExpr());
                if (result.getLanguageError() != null) return result;
                return result.success(val);
            }
            result.registerAdvancement();
            advance();
            return result.success(new StringNode(tok));
        } else if (tok.getType().equals(TokenType.IDENTIFIER)) {
            result.registerAdvancement();
            advance();
            if (currentToken.getType().equals(TokenType.EQUAL))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "111 Should be '=>'"));

            return result.success(new VarAccessNode(tok));
        } else if (tok.getType().equals(TokenType.BOOLEAN)) {
            result.registerAdvancement();
            advance();
            return result.success(new BooleanNode(tok));
        } else if (tok.getType().equals(TokenType.QUESTION_MARK)) {
            Node queryExpr = result.register(this.parseQueryExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(queryExpr);
        } else if (tok.getType().equals(TokenType.LEFT_BRACKET)) {
            Node listExpr = result.register(this.parseListExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(listExpr);
        } else if (tok.getType().equals(TokenType.LEFT_BRACE)) {
            Node dictExpr = result.register(this.parseDictionaryExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(dictExpr);
        } else if (tok.getType().equals(TokenType.LEFT_PAREN)) {
            result.registerAdvancement();
            advance();
            Node expr = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;
            if (currentToken.getType().equals(TokenType.RIGHT_PAREN)) {
                result.registerAdvancement();
                advance();
                return result.success(expr);
            } else
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected ')'"));
        }

        return result.failure(LanguageException.invalidSyntax(tok.getStartPosition().copy(),
                tok.getEndPosition() != null ?
                tok.getEndPosition().copy() : tok.getStartPosition()
                        .advance(tokenFound().length()).copy(),
                String.format("Expected long, double, identifier, '+', '-', or '('. Found %s", tokenFound())));
    }

    @Override
    public ParseResult<Node> parseReferenceExpression() {
        Token prefixToken;
        if (currentToken.getType() == TokenType.STAR || currentToken.getType() == TokenType.AMPERSAND) {
            ParseResult<Node> result = new ParseResult<>();

            prefixToken = currentToken;
            result.registerAdvancement();
            advance();

            Node expr = result.register(parseReferenceExpression());
            if (result.getLanguageError() != null) return result;

            if (prefixToken.getType() == TokenType.STAR) return result.success(new DerefNode(expr));
            else return result.success(new RefNode(expr));
        }

        return index();
    }

    @Override
    public ParseResult<Node> formatStringExpr() {
        ParseResult<Node> result = new ParseResult<>();
        Token tok = currentToken;
        StringBuilder sb = new StringBuilder();
        Pair<String, Boolean> val = WrappedCast.cast(tok.getValue());

        TokenType addToken = TokenType.PLUS;
        Node node = new StringNode(new Token(TokenType.STRING, new Pair<>("", false), tok.getStartPosition(), tok.getEndPosition()));
        for (int i = 0; i < val.getFirst().length(); i++) {
            char current = val.getFirst().charAt(i);
            char next = i + 1 < val.getFirst().length() ? val.getFirst().charAt(i + 1) : ' ';
            if (current == '!' && next == '$') {
                sb.append("$");
                i++;
            } else if (current == '$' && next == '{') {
                node = new BinOpNode(node, addToken, new StringNode(new Token(TokenType.STRING, new Pair<>(sb.toString(), false), tok.getStartPosition(), tok.getEndPosition())));
                sb = new StringBuilder();
                StringBuilder expr = new StringBuilder();
                i += 2;
                while (i < val.getFirst().length() && val.getFirst().charAt(i) != '}') {
                    current = val.getFirst().charAt(i);
                    expr.append(current);
                    i++;
                }

                if (i >= val.getFirst().length())
                    return result.failure(LanguageException.invalidSyntax(tok.getStartPosition(), tok.getEndPosition(), "Unmatched bracket"));

                Lexer lexer = new Lexer("<string>", expr.toString());
                List<Token> expressionStatements = lexer.lex();

                // --
                for (Token tk : expressionStatements) {
                    tk.getStartPosition().setIndex(tk.getStartPosition().getIndex() + tk.getStartPosition().getIndex() + i);
                    tk.getEndPosition().setIndex(tk.getStartPosition().getIndex() + tk.getStartPosition().getIndex() + i);
                }

                Node r = result.register(new RewriteParser(expressionStatements).parseStatement());

                if (result.getLanguageError() != null)
                    return result;

                node = new BinOpNode(node, addToken, r);
                // --

            } else {
                sb.append(current);
            }
        }

        result.registerAdvancement();
        advance();
        return result.success(new BinOpNode(node, addToken, new StringNode(new Token(TokenType.STRING,
                new Pair<>(sb.toString(), false), tok.getStartPosition(), tok.getEndPosition()))));
    }

    @Override
    public ParseResult<Node> parseThrowExpression() {
        ParseResult<Node> result = new ParseResult<>();
        if (!currentToken.matches(TokenType.KEYWORD, "throw"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected 'throw'"));
        result.registerAdvancement();
        advance();

        Node first = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;

        if (currentToken.getType() == TokenType.COMMA) {
            result.registerAdvancement();
            advance();

            Node second = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;

            return result.success(new ThrowNode(first, second));
        }

        return result.success(new ThrowNode(new StringNode(new Token(TokenType.STRING, new Pair<>("Thrown", false), first.getStartPosition(), first.getEndPosition())), first));
    }

    @Override
    public ParseResult<Node> parseStructureDefinition() {
        ParseResult<Node> result = new ParseResult<>();
        if (!currentToken.matches(TokenType.KEYWORD, "structure"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected 'struct'"));

        List<AttributeDeclareNode> childrenDeclarations = new ArrayList<>();
        List<Token> children = new ArrayList<>();
        List<Token> types = new ArrayList<>();
        List<Node> assignment = new ArrayList<>();

        Token identifier = result.register(parseIdentifier("Struct", NamingConvention.PascalCase));
        if (result.getLanguageError() != null) return result;
        result.registerAdvancement();
        advance();

        if (currentToken.getType() != TokenType.LEFT_BRACE)
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '{'"));

        do {
            result.register(parseIdentifier("Attribute name", NamingConvention.CamelCase));
            if (result.getLanguageError() != null) return result;

            children.add(currentToken);
            childrenDeclarations.add(new AttributeDeclareNode(currentToken));
            types.add(new Token(TokenType.TYPE, Collections.singletonList("any"), currentToken.getStartPosition(), currentToken.getEndPosition()));
            assignment.add(new AttributeAssignNode(currentToken, new VarAccessNode(currentToken)));

            result.registerAdvancement();
            advance();
        } while (currentToken.getType() == TokenType.COMMA);

        Position end = currentToken.getEndPosition().copy();

        if (currentToken.getType() != TokenType.RIGHT_BRACE)
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '}'"));
        endLine(1);
        result.registerAdvancement();
        advance();

        return result.success(new ClassDefNode(identifier, childrenDeclarations, children, types, new BodyNode(assignment),
                new ArrayList<>(), end, new ArrayList<>(), 0, null, new ArrayList<>(), null, null));

    }

    @Override
    public ParseResult<Node> parseEnumExpression() {
        ParseResult<Node> result = new ParseResult<>();

        List<EnumChild> children = new ArrayList<>();
        Token name;

        if (!currentToken.matches(TokenType.KEYWORD, "enum"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected 'enum'"));
        result.registerAdvancement();
        advance();

        boolean pub = currentToken.matches(TokenType.KEYWORD, "public");
        if (pub) {
            result.registerAdvancement();
            advance();
        }

        if (currentToken.getType() != TokenType.IDENTIFIER)
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected identifier"));


        name = currentToken;
        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '{'"));
        result.registerAdvancement();
        advance();

        while (currentToken.getType() == TokenType.IDENTIFIER) {
            Token token = currentToken;

            result.registerAdvancement();
            advance();

            List<String> generics = new ArrayList<>();
            if (currentToken.getType() == TokenType.LEFT_PAREN) {
                do {
                    Token identifier = result.register(parseIdentifier("Generic type", NamingConvention.PascalCase));
                    if (result.getLanguageError() != null) return result;
                    result.registerAdvancement();
                    advance();
                    generics.add(identifier.getValue().toString());
                } while (currentToken.getType() == TokenType.COMMA);

                if (currentToken.getType() != TokenType.RIGHT_PAREN)
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                            currentToken.getEndPosition().copy(), "Expected ')'"));
                result.registerAdvancement();
                advance();
            }

            List<String> params = new ArrayList<>();
            List<List<String>> types = new ArrayList<>();
            if (currentToken.getType() == TokenType.LEFT_BRACE) {
                do {
                    Token tok = result.register(parseIdentifier("Parameter", NamingConvention.CamelCase));
                    if (result.getLanguageError() != null) return result;
                    params.add((String) tok.getValue());
                    result.registerAdvancement();
                    advance();

                    if (currentToken.getType() == TokenType.COLON) {
                        result.registerAdvancement();
                        advance();
                        tok = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        types.add(WrappedCast.cast(tok.getValue()));
                    } else {
                        types.add(Collections.singletonList("any"));
                    }

                } while (currentToken.getType() == TokenType.COMMA);
                if (currentToken.getType() != TokenType.RIGHT_BRACE)
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                            currentToken.getEndPosition().copy(), "Expected '}'"));
                result.registerAdvancement();
                advance();
            }

            if (currentToken.getType() != TokenType.COMMA)
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Expected comma"));
            result.registerAdvancement();
            advance();
            children.add(new EnumChild(token, params, types, generics));
        }

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '}'"));
        endLine(1);
        result.registerAdvancement();
        advance();

        return result.success(new EnumNode(name, children, pub));
    }

    @Override
    public ParseResult<Node> parseSwitchExpression() {
        ParseResult<Node> result = new ParseResult<>();

        ElseCase elseCase = null;
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TokenType.KEYWORD, "switch"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected switch"));
        result.registerAdvancement();
        advance();

        Node ref;
        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '('"));
        result.registerAdvancement();
        advance();

        ref = result.register(parseExpression());
        if (result.getLanguageError() != null) return result;
        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ')'"));
        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '{'"));
        result.registerAdvancement();
        advance();

        boolean def;
        Node condition, body;
        while (currentToken.matches(TokenType.KEYWORD, "case") || currentToken.matches(TokenType.KEYWORD, "default")) {
            def = currentToken.matches(TokenType.KEYWORD, "default");
            result.registerAdvancement();
            advance();

            if (!def) {
                condition = result.register(parseComparisonExpression());
                if (result.getLanguageError() != null) return result;
            } else condition = null;

            if (currentToken.getType() != TokenType.COLON)
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Expected ':'"));
            result.registerAdvancement();
            advance();

            body = result.register(statements(Arrays.asList(new TokenMatcher(TokenType.RIGHT_BRACE, null),
                    new TokenMatcher(TokenType.KEYWORD, "case"), new TokenMatcher(TokenType.KEYWORD, "default"))));
            if (result.getLanguageError() != null) return result;

            if (def) elseCase = new ElseCase(body, false);
            else cases.add(new Case(condition, body, false));
        }

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '}'"));

        endLine(1);
        result.registerAdvancement();
        advance();

        Node switchNode = new SwitchNode(ref, cases, elseCase, false);
        return result.success(switchNode);
    }

    @Override
    public ParseResult<Void> expectSemicolon() {
        if (currentToken.getType() != TokenType.INVISIBLE_NEW_LINE && currentToken.getType() != TokenType.NEW_LINE)
            return new ParseResult<Void>().failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ';'"));

        return new ParseResult<>();
    }

    @Override
    public ParseResult<Node> patternExpr(Node expr) {
        ParseResult<Node> result = new ParseResult<>();
        HashMap<Token, Node> patterns = new HashMap<>();

        if (currentToken.getType() != TokenType.LEFT_PAREN)
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '('"));

        if (peek(1).getType() == TokenType.IDENTIFIER) do {
            Token identifier = result.register(parseIdentifier());
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            advance();

            if (currentToken.getType() != TokenType.COLON) {
                patterns.put(identifier, new VarAccessNode(identifier));
                continue;
            }
            result.registerAdvancement();
            advance();

            Node pattern = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;

            patterns.put(identifier, pattern);
        } while (currentToken.getType() == TokenType.COMMA);
        else {
            result.registerAdvancement();
            advance();
        }

        if (currentToken.getType() != TokenType.RIGHT_PAREN)
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ')'"));

        result.registerAdvancement();
        advance();

        return result.success(new PatternNode(expr, patterns));
    }

    @Override
    public ParseResult<Node> parseMatchingExpression() {
        ParseResult<Node> result = new ParseResult<>();

        ElseCase elseCase = null;
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TokenType.KEYWORD, "match"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected match"));

        result.registerAdvancement();
        advance();

        Node ref;
        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '('"));

        result.registerAdvancement();
        advance();
        ref = result.register(parseExpression());
        if (result.getLanguageError() != null) return result;
        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ')'"));

        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '{'"));

        result.registerAdvancement();
        advance();

        Node body;
        boolean pat, def;
        while (currentToken.getType() != TokenType.RIGHT_BRACE) {
            pat = !currentToken.matches(TokenType.KEYWORD, "case") && !currentToken.matches(TokenType.KEYWORD, "default");
            def = currentToken.matches(TokenType.KEYWORD, "default");

            List<Node> conditions = new ArrayList<>();
            reverse(1);
            do {
                result.registerAdvancement();
                advance();
                Node condition;
                if (pat) {
                    condition = result.register(atom());
                    if (result.getLanguageError() != null) return result;
                    if (currentToken.getType() == TokenType.LEFT_PAREN) {
                        condition = result.register(patternExpr(condition));
                        if (result.getLanguageError() != null) return result;
                    }
                } else {
                    result.registerAdvancement();
                    advance();
                    if (!def) {
                        condition = result.register(parseStatement());
                        if (result.getLanguageError() != null) return result;
                    } else condition = null;
                }
                if (condition != null) conditions.add(condition);
            } while (currentToken.getType() == TokenType.COMMA);

            if (currentToken.getType() != TokenType.SKINNY_ARROW)
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Expected '->'"));

            result.registerAdvancement();
            advance();

            body = result.register(parseStatement());
            if (result.getLanguageError() != null) return result;

            if (def) elseCase = new ElseCase(body, true);
            else for (Node condition : conditions)
                cases.add(new Case(condition, body, true));
            result.register(expectSemicolon());
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            advance();
        }

        result.registerAdvancement();
        advance();

        Node switchNode = new SwitchNode(ref, cases, elseCase, true);
        return result.success(switchNode);
    }

    @Override
    public ParseResult<Node> call() {
        ParseResult<Node> result = new ParseResult<>();
        Node node = result.register(this.atom());

        if (result.getLanguageError() != null) return result;                                               // double ::
        while (currentToken.getType().equals(TokenType.LEFT_PAREN) || currentToken.getType().equals(TokenType.DOT)) {
            if (currentToken.getType().equals(TokenType.LEFT_PAREN)) {
                List<Token> generics = new ArrayList<>();
                List<Node> argumentNodes = new ArrayList<>();
                Map<String, Node> keywordArguments = new HashMap<>();
                if (peek(1) != null && !peek(1).getType().equals(TokenType.RIGHT_PAREN)) {
                    if (peek(1).getType() != TokenType.BACK_SLASH) {
                        do {
                            result.registerAdvancement();
                            advance();

                            if (currentToken.getType() == TokenType.DOT_DOT) {
                                result.registerAdvancement();
                                advance();
                                Node internal = result.register(this.parseExpression());
                                if (result.getLanguageError() != null) return result;
                                argumentNodes.add(new SpreadNode(internal));
                            } else {
                                argumentNodes.add(result.register(this.parseExpression()));
                                if (result.getLanguageError() != null) return result;
                            }
                        } while (currentToken.getType().equals(TokenType.COMMA));
                    } else {
                        result.registerAdvancement();
                        advance();
                    }

                    if (currentToken.getType() == TokenType.BACK_SLASH) {
                        Token vk;
                        Node val;
                        do {
                            vk = result.register(parseIdentifier());
                            if (result.getLanguageError() != null) return result;
                            result.registerAdvancement();
                            advance();

                            if (currentToken.getType() != TokenType.COLON)
                                return result.failure(LanguageException.expected(currentToken.getStartPosition(),
                                        currentToken.getEndPosition(), "Expected ':'"));
                            result.registerAdvancement();
                            advance();

                            val = result.register(this.parseExpression());
                            if (result.getLanguageError() != null) return result;

                            keywordArguments.put(vk.getValue().toString(), val);
                        } while (currentToken.getType() == TokenType.COMMA);
                    }

                    if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
                        return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                                currentToken.getEndPosition().copy(), "Expected ',' or ')'"));
                } else {
                    result.registerAdvancement();
                    advance();
                }
                result.registerAdvancement();
                advance();
                if (currentToken.getType() == TokenType.LEFT_ANGLE && TYPE_TOKENS.contains(peek(1).getType())) {
                    int startIndex = tokenIndex;
                    result.registerAdvancement();
                    advance();

                    ParseResult<Token> typetoken = parseTypeToken();
                    if (typetoken.getLanguageError() != null) return result.failure(typetoken.getLanguageError());
                    generics.add(result.register(typetoken));

                    while (currentToken.getType() == TokenType.COMMA) {
                        result.registerAdvancement();
                        advance();

                        typetoken = parseTypeToken();
                        if (typetoken.getLanguageError() != null) return result.failure(typetoken.getLanguageError());
                        generics.add(result.register(typetoken));
                    }
                    if (currentToken.getType() != TokenType.RIGHT_ANGLE) {
                        generics = new ArrayList<>();
                        tokenIndex = startIndex;
                        updateTokens();
                    } else {
                        result.registerAdvancement();
                        advance();
                    }
                }

                node = new CallNode(node, argumentNodes, generics, keywordArguments);
            } else {
                Token tok = result.register(parseIdentifier());

                if (result.getLanguageError() != null) return result;
                advance();
                result.registerAdvancement();
                node = new ClassAccessNode(node, tok);
            }
        }
        return result.success(node);
    }

    @Override
    public ParseResult<Node> index() {
        return binOp(this::call, Collections.singletonList(TokenType.LEFT_BRACKET), this::parseExpression);
    }

    @Override
    public ParseResult<Node> pow() {
        return binOp(this::refOp, Arrays.asList(TokenType.CARET, TokenType.PERCENT), this::parseFactor);
    }

    @Override
    public ParseResult<Node> refOp() {
        return binOp(this::refSugars, Collections.singletonList(TokenType.FAT_ARROW));
    }

    @Override
    public ParseResult<Node> refSugars() {
        ParseResult<Node> result = new ParseResult<>();

        Node expr = result.register(this.parseReferenceExpression());
        if (result.getLanguageError() != null) return result;

        while (binRefOps.contains(currentToken.getType()) || unRefOps.contains(currentToken.getType())) {
            if (unRefOps.contains(currentToken.getType())) {
                expr = new BinOpNode(expr, TokenType.FAT_ARROW, new UnaryOpNode(currentToken.getType(), expr));

                result.registerAdvancement();
                advance();
            } else if (binRefOps.contains(currentToken.getType())) {
                TokenType opTok = switch (currentToken.getType()) {
                    case CARET_EQUALS -> TokenType.CARET;
                    case START_EQUALS -> TokenType.STAR;
                    case SLASH_EQUALS -> TokenType.SLASH;
                    case PLUS_EQUALS -> TokenType.PLUS;
                    case MINUS_EQUALS -> TokenType.MINUS;
                    default -> null;
                };
                result.registerAdvancement();
                advance();

                Node right = result.register(this.parseExpression());
                if (result.getLanguageError() != null) return result;

                expr = new BinOpNode(expr, TokenType.FAT_ARROW, new BinOpNode(expr, opTok, right));
            }
        }

        return result.success(expr);
    }

    @Override
    public ParseResult<Node> parseTerm() {
        return binOp(this::parseFactor, Arrays.asList(TokenType.STAR, TokenType.SLASH));
    }

    @Override
    public ParseResult<Node> parseAttribute() {
        return binOp(this::parseTerm, Arrays.asList(TokenType.PLUS, TokenType.MINUS));
    }

    @Override
    public ParseResult<Node> parseComparisonExpression() {
        ParseResult<Node> result = new ParseResult<>();
        if (currentToken.getType().equals(TokenType.BANG)) {
            Token operationToken = currentToken;
            result.registerAdvancement();
            advance();

            Node node = result.register(parseComparisonExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(new UnaryOpNode(operationToken.getType(), node));
        }
        Node node = result.register(binOp(this::parseAttribute, Arrays.asList(TokenType.EQUAL_EQUAL,
                TokenType.BANG_EQUAL, TokenType.LEFT_ANGLE, TokenType.RIGHT_ANGLE, TokenType.LESS_EQUALS, TokenType.GREATER_EQUALS)));

        if (result.getLanguageError() != null) return result;
        return result.success(node);
    }

    @Override
    public ParseResult<Node> parseBitExpression() {
        ParseResult<Node> result = new ParseResult<>();
        Node node = result.register(binOp(this::parseBitShiftExpression, Arrays.asList(TokenType.AMPERSAND, TokenType.PIPE)));
        if (result.getLanguageError() != null) return result;
        return result.success(node);
    }

    @Override
    public ParseResult<Node> binOp(ParseExecutable leftParse, List<TokenType> ops) {
        return binOp(leftParse, ops, null);
    }

    @Override
    public ParseResult<Node> binOp(ParseExecutable leftParse, List<TokenType> ops, ParseExecutable rightParse) {
        ParseResult<Node> result = new ParseResult<>();
        if (rightParse == null) rightParse = leftParse;
        Node right;
        Node left;
        left = result.register(leftParse.execute());
        if (result.getLanguageError() != null) return result;

        while (ops.contains(currentToken.getType())) {
            TokenType operationToken = currentToken.getType();
            result.registerAdvancement();
            advance();
            right = result.register(rightParse.execute());
            if (result.getLanguageError() != null) return result;
            if (operationToken == TokenType.LEFT_BRACKET) {
                if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                            currentToken.getEndPosition().copy(), "Expected closing bracket (']')"));
                advance();
                result.registerAdvancement();
            }
            if (operationToken == TokenType.DOT && right.getNodeType() == NodeType.CALL) {
                CallNode call = (CallNode) right;
                call.argNodes.add(0, left);
                left = call;
            } else left = new BinOpNode(left, operationToken, right);
        }
        return result.success(left);
    }

    @Override
    public ParseResult<Argument> parseArguments() {
        ParseResult<Argument> result = new ParseResult<>();

        List<Token> argNameTokens = new ArrayList<>();
        List<Token> argTypeTokens = new ArrayList<>();

        String argumentName = null;
        String keywordArgument = null;

        List<Node> defaults = new ArrayList<>();
        int defaultCount = 0;
        boolean optionals = false;

        if (currentToken.getType().equals(TokenType.LEFT_PAREN) && peek(1).getType().equals(TokenType.RIGHT_PAREN)) {
            this.advance(); // (
            this.advance(); // )
            // ignore
        } else if (currentToken.getType().equals(TokenType.LEFT_PAREN)) {
            if (peek(1) != null && peek(1).getType() != TokenType.BACK_SLASH && peek(1).getType() != TokenType.RIGHT_ANGLE) {
                boolean args;
                do {
                    result.registerAdvancement();
                    advance();

                    args = currentToken.getType() == TokenType.DOT_DOT;
                    if (args) {
                        result.registerAdvancement();
                        advance();
                    }

                    if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                                currentToken.getEndPosition().copy(), "Expected identifier"));


                    if (args) {
                        argumentName = currentToken.getValue().toString();
                        result.registerAdvancement();
                        advance();
                        break;
                    }

                    argNameTokens.add(currentToken);
                    result.registerAdvancement();
                    advance();

                    // Syntactic sugar for function annotations
                    // argument<int, int>: int
                    // argument: int<int, int>
                    if (currentToken.getType().equals(TokenType.LEFT_ANGLE)) {
                        Token typeToken = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        List<String> type = WrappedCast.cast(typeToken.getValue());
                        // Expect :
                        if (currentToken.getType() != TokenType.COLON)
                            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                                    currentToken.getEndPosition().copy(), "Expected ':' after type annotation"));
                        result.registerAdvancement();
                        advance();
                        Token calledTypeTok = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        List<String> calledType = WrappedCast.cast(calledTypeTok.getValue());
                        calledType.addAll(type);
                        argTypeTokens.add(calledTypeTok);
                    } else if (currentToken.getType().equals(TokenType.COLON)) {
                        result.registerAdvancement();
                        advance();

                        Token typeToken = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        argTypeTokens.add(typeToken);
                    } else
                        argTypeTokens.add(new Token(TokenType.TYPE, Collections.singletonList("any"),
                                currentToken.getStartPosition(), currentToken.getEndPosition()));
                    if (currentToken.getType().equals(TokenType.EQUAL)) {
                        result.registerAdvancement();
                        advance();

                        Node val = result.register(parseAttribute());
                        if (result.getLanguageError() != null) return result;

                        defaults.add(val);
                        defaultCount++;
                        optionals = true;
                    } else if (optionals)
                        return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                                currentToken.getEndPosition().copy(), "Expected default value"));

                } while (currentToken.getType().equals(TokenType.COMMA));
            } else {
                result.registerAdvancement();
                advance();
            }

            if (currentToken.getType() == TokenType.BACK_SLASH) {
                Token kw = result.register(parseIdentifier("Parameter", NamingConvention.CamelCase));
                if (result.getLanguageError() != null) return result;
                result.registerAdvancement();
                advance();
                keywordArgument = kw.getValue().toString();
            }

            if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Expected ')'"));
            advance();
            result.registerAdvancement();
        }

        List<Token> generics = new ArrayList<>();
        if (currentToken.getType().equals(TokenType.LEFT_ANGLE)) {
            result.registerAdvancement();
            advance();
            do {
                if (currentToken.getType() == TokenType.COMMA) {
                    result.registerAdvancement();
                    advance();
                }
                if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                    return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                            currentToken.getEndPosition().copy(), "Expected type"));


                generics.add(currentToken);
                result.registerAdvancement();
                advance();
            } while (currentToken.getType() == TokenType.COMMA);
            if (!currentToken.getType().equals(TokenType.RIGHT_ANGLE))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Expected ')'"));
            result.registerAdvancement();
            advance();
        }

        return result.success(new Argument(argNameTokens, argTypeTokens, defaults, defaultCount, generics, argumentName, keywordArgument));
    }

    @Override
    public void endLine(int offset) {
        tokens.add(tokenIndex + offset, new Token(TokenType.INVISIBLE_NEW_LINE,
                currentToken.getStartPosition().copy(), currentToken.getStartPosition().copy()));
        tokenCount++;
    }

    @Override
    public ParseResult<Node> parseBlock() {
        return parseBlock(true);
    }

    @Override
    public ParseResult<Node> parseBlock(boolean vLine) {
        ParseResult<Node> result = new ParseResult<>();
        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '{'"));

        result.registerAdvancement();
        advance();

        Node statements = result.register(this.statements(Collections.singletonList(new TokenMatcher(TokenType.RIGHT_BRACE, null))));
        if (result.getLanguageError() != null) return result;

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '}'"));
        if (vLine) endLine(1);
        result.registerAdvancement();
        advance();

        return result.success(statements);
    }

    // EXPRESSIONS

    // If Parts

    @Override
    public ParseResult<Node> parseIfExpression() {
        ParseResult<Node> result = new ParseResult<>();
        Pair<List<Case>, ElseCase> allCases = result.register(this.parseIfCases("if", true));
        if (result.getLanguageError() != null) return result;
        List<Case> cases = allCases.getFirst();
        ElseCase elseCase = allCases.getLast();
        endLine(0);
        updateTokens();
        return result.success(new QueryNode(cases, elseCase));
    }

    @Override
    public ParseResult<Pair<List<Case>, ElseCase>> parseIfCases(String caseKeyword, boolean parenthesis) {
        ParseResult<Pair<List<Case>, ElseCase>> result = new ParseResult<>();
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TokenType.KEYWORD, caseKeyword))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), String.format("Expected %s", caseKeyword)));

        result.registerAdvancement();
        advance();

        if (parenthesis) {
            if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '('"));
            result.registerAdvancement();
            advance();
        }
        Node condition = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;

        if (parenthesis) {
            if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected ')'"));
            result.registerAdvancement();
            advance();
        }

        Node statements;
        if (currentToken.getType().equals(TokenType.LEFT_BRACE)) statements = result.register(this.parseBlock(false));
        else {
            statements = result.register(this.parseStatement());
            result.register(expectSemicolon());
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            advance();
        }
        if (result.getLanguageError() != null) return result;
        cases.add(new Case(condition, statements, false));

        Pair<List<Case>, ElseCase> allCases = result.register(this.parseElseIfStatement(parenthesis));
        if (result.getLanguageError() != null) return result;
        List<Case> newCases = allCases.getFirst();
        ElseCase elseCase = allCases.getLast();
        cases.addAll(newCases);

        return result.success(new Pair<>(cases, elseCase));
    }

    @Override
    public ParseResult<Pair<List<Case>, ElseCase>> parseElseIfStatement(boolean parenthesis) {
        ParseResult<Pair<List<Case>, ElseCase>> result = new ParseResult<>();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase;

        if (currentToken.matches(TokenType.KEYWORD, "elseIf")) {
            Pair<List<Case>, ElseCase> allCases = result.register(this.parseElifExpression(parenthesis));
            if (result.getLanguageError() != null) return result;
            cases = allCases.getFirst();
            elseCase = allCases.getLast();
        } else {
            elseCase = result.register(this.elseExpr());
            if (result.getLanguageError() != null) return result;
        }
        return result.success(new Pair<>(cases, elseCase));

    }

    @Override
    public ParseResult<Pair<List<Case>, ElseCase>> parseElifExpression(boolean parenthesis) {
        return parseIfCases("elseIf", parenthesis);
    }

    @Override
    public ParseResult<ElseCase> elseExpr() {
        ParseResult<ElseCase> result = new ParseResult<>();
        ElseCase elseCase = null;

        if (currentToken.matches(TokenType.KEYWORD, "else")) {
            result.registerAdvancement();
            advance();

            Node statements;
            if (currentToken.getType().equals(TokenType.LEFT_BRACE)) statements = result.register(this.parseBlock());
            else {
                statements = result.register(this.parseStatement());
                result.register(expectSemicolon());
                if (result.getLanguageError() != null) return result;
                result.registerAdvancement();
                advance();
            }
            if (result.getLanguageError() != null) return result;
            elseCase = new ElseCase(statements, false);
        }

        return result.success(elseCase);
    }

    // Query

    @Override
    public ParseResult<Node> kv() {
        ParseResult<Node> result = new ParseResult<>();
        if (!currentToken.getType().equals(TokenType.COLON))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ':'"));
        result.registerAdvancement();
        advance();
        Node expr = result.register(parseExpression());
        if (result.getLanguageError() != null) return result;
        return result.success(expr);
    }

    @Override
    public ParseResult<Node> parseQueryExpression() {
        ParseResult<Node> result = new ParseResult<>();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase = null;

        ParseExecutable getStatement = () -> {
            result.registerAdvancement();
            advance();
            Node condition = result.register(parseComparisonExpression());
            if (result.getLanguageError() != null) return result;
            Node expr_ = result.register(this.kv());
            if (result.getLanguageError() != null) return result;
            cases.add(new Case(condition, expr_, true));
            return null;
        };

        if (!currentToken.getType().equals(TokenType.QUESTION_MARK))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '?'"));

        ParseResult<Node> r;
        r = getStatement.execute();
        if (r != null) return r;

        while (currentToken.getType().equals(TokenType.DOLLAR)) {
            r = getStatement.execute();
            if (r != null) return r;
        }

        if (currentToken.getType().equals(TokenType.DOLLAR_UNDER_SCORE)) {
            result.registerAdvancement();
            advance();
            if (!currentToken.getType().equals(TokenType.COLON))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected ':'"));
            result.registerAdvancement();
            advance();

            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            elseCase = new ElseCase(expr, true);
        }
        return result.success(new QueryNode(cases, elseCase));
    }

    // Loops

    @Override
    public ParseResult<Node> parseForLoop() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "for"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected 'for'"));

        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '('"));
        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected identifier"));


        Token varName = currentToken;
        result.registerAdvancement();
        advance();

        boolean iterating = currentToken.getType().equals(TokenType.LEFT_ARROW);
        if (!currentToken.getType().equals(TokenType.SKINNY_ARROW) && !currentToken.getType().equals(TokenType.LEFT_ARROW))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected weak assignment or iterator ('->', '<-')"));
        result.registerAdvancement();
        advance();

        if (iterating) {
            Node iterableNode = result.register(getClosing());
            if (result.getLanguageError() != null) return result;
            Node body;
            switch (currentToken.getType()) {
                case LEFT_BRACE -> {
                    body = result.register(this.parseBlock());
                    if (result.getLanguageError() != null) return result;
                    return result.success(new IterNode(varName, iterableNode, body, true));
                }
                case FAT_ARROW -> {
                    result.registerAdvancement();
                    advance();
                    body = result.register(this.parseStatement());
                    if (result.getLanguageError() != null) return result;
                    return result.success(new IterNode(varName, iterableNode, body, false));
                }
                default -> {
                    body = result.register(this.parseStatement());
                    if (result.getLanguageError() != null) return result;
                    return result.success(new IterNode(varName, iterableNode, body, true));
                }
            }
        }
        Node start = result.register(this.parseComparisonExpression());
        if (result.getLanguageError() != null) return result;

        if (!currentToken.getType().equals(TokenType.COLON))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ':'"));
        result.registerAdvancement();
        advance();

        Node end = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;

        Node step;
        if (currentToken.getType().equals(TokenType.ANGLE_ANGLE)) {
            result.registerAdvancement();
            advance();
            step = result.register(this.parseExpression());
        } else step = null;

        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ')'"));
        result.registerAdvancement();
        advance();

        Node body;
        switch (currentToken.getType()) {
            case LEFT_BRACE -> {
                body = result.register(this.parseBlock());
                if (result.getLanguageError() != null) return result;
                return result.success(new ForNode(varName, start, end, step, body, true));
            }
            case FAT_ARROW -> {
                result.registerAdvancement();
                advance();
                body = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new ForNode(varName, start, end, step, body, false));
            }
            default -> {
                body = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new ForNode(varName, start, end, step, body, true));
            }
        }

    }

    @Override
    public ParseResult<Node> getClosing() {
        ParseResult<Node> result = new ParseResult<>();
        Node condition = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;
        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ')'"));
        result.registerAdvancement();
        advance();
        return result.success(condition);
    }

    @Override
    public ParseResult<Node> parseWhileLoop() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "while"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected 'while'"));
        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '('"));
        result.registerAdvancement();
        advance();

        Node condition = result.register(getClosing());
        if (result.getLanguageError() != null) return result;

        return result.success(condition);
    }

    @Override
    public ParseResult<Node> parseDoExpression() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "do"))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected 'do'"));
        result.registerAdvancement();
        advance();

        Node body;
        boolean bracket = currentToken.getType() == TokenType.LEFT_BRACE;
        switch (currentToken.getType()) {
            case FAT_ARROW -> {
                result.registerAdvancement();
                advance();
                body = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
            }
            case LEFT_BRACE -> {
                body = result.register(parseBlock(false));
                if (result.getLanguageError() != null) return result;
            }
            default -> {
                body = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
            }
        }

        Node condition = result.register(parseWhileLoop());
        if (result.getLanguageError() != null) return result;

        return result.success(new WhileNode(condition, body, bracket, true));
    }

    @Override
    public ParseResult<Node> parseWhileExpression() {
        ParseResult<Node> result = new ParseResult<>();

        Node condition;
        if (currentToken.matches(TokenType.KEYWORD, "loop")) {
            Token loopTok = currentToken;
            result.registerAdvancement();
            advance();
            condition = new BooleanNode(new Token(TokenType.BOOLEAN, true, loopTok.getStartPosition(), loopTok.getEndPosition()));
        } else {
            condition = result.register(parseWhileLoop());

            if (result.getLanguageError() != null) return result;
        }
        Node body;
        switch (currentToken.getType()) {
            case FAT_ARROW -> {
                result.registerAdvancement();
                advance();
                body = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new WhileNode(condition, body, false, false));
            }
            case LEFT_BRACE -> {
                body = result.register(parseBlock());
                if (result.getLanguageError() != null) return result;
                return result.success(new WhileNode(condition, body, true, false));
            }
            default -> {
                body = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new WhileNode(condition, body, true, false));
            }
        }

    }

    // Collections

    @Override
    public ParseResult<Node> parseListExpression() {
        ParseResult<Node> result = new ParseResult<>();
        List<Node> elementNodes = new ArrayList<>();
        Position start = currentToken.getStartPosition().copy();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACKET))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '['"));
        result.registerAdvancement();
        advance();

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACKET)) {
            elementNodes.add(result.register(this.parseExpression()));
            if (result.getLanguageError() != null) return result;

            while (currentToken.getType().equals(TokenType.COMMA)) {
                result.registerAdvancement();
                advance();
                elementNodes.add(result.register(this.parseExpression()));
                if (result.getLanguageError() != null) return result;
            }
            if (!currentToken.getType().equals(TokenType.RIGHT_BRACKET))
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected ']'"));
        }
        result.registerAdvancement();
        advance();
        return result.success(new ListNode(elementNodes, start, currentToken.getEndPosition().copy()));
    }

    @Override
    public ParseResult<Node> parseDictionaryExpression() {
        ParseResult<Node> result = new ParseResult<>();
        Map<Node, Node> dict = new HashMap<>();
        Position start = currentToken.getStartPosition().copy();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '{'"));
        result.registerAdvancement();
        advance();

        ParseExecutable kv = () -> {
            Node key = result.register(parseComparisonExpression());
            if (result.getLanguageError() != null) return result;

            Node value = result.register(this.kv());
            if (result.getLanguageError() != null) return result;
            dict.put(key, value);
            return null;
        };

        ParseResult<Node> x;
        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE)) {
            x = kv.execute();
            if (x != null) return x;
        }

        while (currentToken.getType().equals(TokenType.COMMA)) {
            advance();
            result.registerAdvancement();
            x = kv.execute();
            if (x != null) return x;
        }
        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '}'"));
        result.registerAdvancement();
        advance();

        return result.success(new MapNode(dict, start, currentToken.getEndPosition().copy()));
    }

    @Override
    public ParseResult<Boolean> isCatcher() {
        ParseResult<Boolean> result = new ParseResult<>();
        if (currentToken.getType() == TokenType.LEFT_BRACKET) {
            result.registerAdvancement();
            advance();
            if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                return result.failure(LanguageException.expected(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected ']'"));
            result.registerAdvancement();
            advance();
            return result.success(true);
        }
        return result.success(false);
    }

    // Executables

    @Override
    public ParseResult<Node> parseFunctionDefinition() {
        ParseResult<Node> result = new ParseResult<>();

        String tokV = (String) currentToken.getValue();
        if (!currentToken.getType().equals(TokenType.KEYWORD) && Arrays.asList("fn", "function").contains(tokV))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected 'function'"));
        advance();
        result.registerAdvancement();

        boolean async = false;
        if (currentToken.matches(TokenType.KEYWORD, "async")) {
            async = true;
            advance();
            result.registerAdvancement();
        }

        Token varNameTok = null;
        if (currentToken.getType().equals(TokenType.IDENTIFIER)) {
            varNameTok = currentToken;

            result.registerAdvancement();
            advance();
        }

        Argument argTKs = result.register(parseArguments());
        if (result.getLanguageError() != null) return result;

        boolean isCatcher = result.register(this.isCatcher());
        if (result.getLanguageError() != null) return result;

        List<String> retype = result.register(parseStaticReturn());
        if (result.getLanguageError() != null) return result;

        Node nodeToReturn;
        switch (currentToken.getType()) {
            case FAT_ARROW -> {
                result.registerAdvancement();
                advance();
                nodeToReturn = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new FunctionDeclareNode(varNameTok, argTKs.argumentTokenNames, argTKs.argumentTypeTokens, nodeToReturn,
                        true, async, retype, argTKs.defaults, argTKs.defaultCount, argTKs.generics, argTKs.argumentName, argTKs.argumentName).setCatcher(isCatcher));
            }
            case LEFT_BRACE -> {
                nodeToReturn = result.register(this.parseBlock(varNameTok != null));
                if (result.getLanguageError() != null) return result;

                Node funcNode = new FunctionDeclareNode(varNameTok, argTKs.argumentTokenNames, argTKs.argumentTypeTokens, nodeToReturn,
                        false, async, retype, argTKs.defaults, argTKs.defaultCount, argTKs.generics, argTKs.argumentName, argTKs.argumentName).setCatcher(isCatcher);

                return result.success(funcNode);
            }
            default -> {
                return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '->' or '{'"));
            }
        }

    }

    @Override
    public ParseResult<List<String>> parseStaticReturn() {
        ParseResult<List<String>> result = new ParseResult<>();

        List<String> retype = Collections.singletonList("any");

        // $_
        if (currentToken.getType().equals(TokenType.SKINNY_ARROW) ||
                currentToken.matches(TokenType.KEYWORD, "yields") || currentToken.getType().equals(TokenType.COLON)) {

            result.registerAdvancement();
            advance();
            Token typeToken = result.register(parseTypeToken());
            if (result.getLanguageError() != null) return result;

            retype = WrappedCast.cast(typeToken.getValue());

        }
        return result.success(retype);
    }

    @Override
    public ParseResult<Node> parseClassDefinition() {
        ParseResult<Node> result = new ParseResult<>();

        if (currentToken.getType() != TokenType.KEYWORD || !classKeywords.contains(currentToken.getValue().toString()))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected 'recipe', 'class', or 'obj'"));
        advance();
        result.registerAdvancement();
        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected identifier"));


        Token classNameTok = currentToken;
        result.registerAdvancement();
        advance();

        Token ptk = null;
        if (currentToken.getType() == TokenType.SKINNY_ARROW) {
            advance();
            result.registerAdvancement();
            if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected identifier"));
            ptk = currentToken;
            result.registerAdvancement();
            advance();
        }


        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '{'"));
        result.registerAdvancement();
        advance();

        List<AttributeDeclareNode> attributeDeclarations = new ArrayList<>();

        Argument argument = new Argument(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, new ArrayList<>(), null, null);

        AttributeGetter getComplexAttribute = (valTok, isStatic, isPrivate) -> {
            ParseResult<Node> attributeResult = new ParseResult<>();

            List<String> type = Collections.singletonList("any");
            if (currentToken.getType() == TokenType.COLON) {
                attributeResult.registerAdvancement();
                advance();
                Token t = attributeResult.register(parseTypeToken());
                if (attributeResult.getLanguageError() != null) return attributeResult;
                type = WrappedCast.cast(t.getValue());
            }

            Node expr = null;
            if (currentToken.getType().equals(TokenType.FAT_ARROW))
                return attributeResult.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Should be '='"));
            if (currentToken.getType() == TokenType.EQUAL) {
                attributeResult.registerAdvancement();
                advance();
                expr = attributeResult.register(this.parseExpression());
                if (attributeResult.getLanguageError() != null) return attributeResult;
            }

            attributeDeclarations.add(new AttributeDeclareNode(valTok, type, isStatic, isPrivate, expr));

            attributeResult.register(expectSemicolon());
            if (attributeResult.getLanguageError() != null) return attributeResult;
            advance();
            attributeResult.registerAdvancement();

            return attributeResult;
        };

        Node ingredientNode = new BodyNode(new ArrayList<>(), classNameTok.getStartPosition().copy(), classNameTok.getEndPosition().copy());
        List<MethodDeclareNode> methods = new ArrayList<>();
        while (currentToken.getType().equals(TokenType.KEYWORD) || currentToken.getType().equals(TokenType.IDENTIFIER)) {
            if (constructorWords.contains(currentToken.getValue().toString())) {
                advance();
                result.registerAdvancement();
                argument = result.register(parseArguments());
                if (result.getLanguageError() != null) return result;

                ingredientNode = result.register(this.parseBlock(false));
                if (result.getLanguageError() != null) return result;
            } else if ("method".equals(currentToken.getValue().toString())) {
                result.registerAdvancement();
                advance();

                boolean async, bin, isStatic, isPrivate;
                async = bin = isStatic = isPrivate = false;
                while (currentToken.getType().equals(TokenType.KEYWORD)) {
                    switch (currentToken.getValue().toString()) {
                        case "bin" -> bin = true;
                        case "async" -> async = true;
                        case "static" -> isStatic = true;
                        case "private" -> isPrivate = true;
                        case "public" -> isPrivate = false;
                    }
                    advance();
                    result.registerAdvancement();
                }

                if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                    return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected identifier"));
                Token varNameTok = currentToken;


                result.registerAdvancement();
                advance();
                Argument args = result.register(parseArguments());

                boolean isCatcher = result.register(this.isCatcher());
                if (result.getLanguageError() != null) return result;

                List<String> retype = result.register(parseStaticReturn());
                if (result.getLanguageError() != null) return result;

                Node nodeToReturn;
                switch (currentToken.getType()) {
                    case FAT_ARROW -> {
                        result.registerAdvancement();
                        advance();
                        nodeToReturn = result.register(this.parseStatement());
                        if (result.getLanguageError() != null) return result;
                        result.register(expectSemicolon());
                        if (result.getLanguageError() != null) return result;
                        result.registerAdvancement();
                        advance();
                        methods.add(new MethodDeclareNode(varNameTok, args.argumentTokenNames, args.argumentTypeTokens, nodeToReturn, true, bin, async, retype, args.defaults, args.defaultCount, args.generics, isStatic, isPrivate, args.argumentName, args.keywordArgument).setCatcher(isCatcher));
                    }
                    case LEFT_BRACE -> {
                        nodeToReturn = result.register(this.parseBlock(false));
                        if (result.getLanguageError() != null) return result;
                        methods.add(new MethodDeclareNode(varNameTok, args.argumentTokenNames, args.argumentTypeTokens, nodeToReturn, false, bin, async, retype, args.defaults, args.defaultCount, args.generics, isStatic, isPrivate, args.argumentName, args.keywordArgument).setCatcher(isCatcher));
                    }
                    default -> {
                        return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '{' or '->'"));
                    }
                }

            } else if (currentToken.getType().equals(TokenType.IDENTIFIER)) {
                Token valTok = currentToken;


                result.registerAdvancement();
                advance();
                if (currentToken.getType().equals(TokenType.FAT_ARROW))
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Should be '='"));
                if (currentToken.getType() == TokenType.EQUAL || currentToken.getType() == TokenType.COLON) {
                    result.register(getComplexAttribute.run(valTok, false, false));
                    if (result.getLanguageError() != null) return result;
                } else {
                    attributeDeclarations.add(new AttributeDeclareNode(valTok));
                    while (currentToken.getType().equals(TokenType.COMMA)) {
                        result.registerAdvancement();
                        advance();
                        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                            return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected identifier"));


                        attributeDeclarations.add(new AttributeDeclareNode(currentToken));
                        advance();
                        result.registerAdvancement();
                    }

                    result.register(expectSemicolon());
                    if (result.getLanguageError() != null) return result;
                    advance();
                    result.registerAdvancement();
                }
            } else if (declarationKeywords.contains(currentToken.getValue().toString())) {
                boolean isPrivate, isStatic;
                isPrivate = isStatic = false;

                while (declarationKeywords.contains(currentToken.getValue().toString())) {
                    switch (currentToken.getValue().toString()) {
                        case "private" -> isPrivate = true;
                        case "static" -> isStatic = true;
                        case "public" -> isPrivate = false;
                    }
                    result.registerAdvancement();
                    advance();
                }

                if (currentToken.getType() != TokenType.IDENTIFIER)
                    return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(), currentToken.getEndPosition(), "Expected identifier"));

                Token valTok = currentToken;


                result.registerAdvancement();
                advance();
                result.register(getComplexAttribute.run(valTok, isStatic, isPrivate));
                if (result.getLanguageError() != null) return result;
            } else
                return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(),
                        currentToken.getEndPosition(), "Unexpected keyword: " + currentToken.asString()));
        }
        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Expected '}'"));
        endLine(1);
        advance();
        result.registerAdvancement();

        Node classDef = new ClassDefNode(classNameTok, attributeDeclarations, argument.argumentTokenNames, argument.argumentTypeTokens, ingredientNode, methods, currentToken.getEndPosition().copy(), argument.defaults, argument.defaultCount, ptk, argument.generics, argument.argumentName, argument.keywordArgument);

        return result.success(classDef);
    }

    // --- utility methods ---

    public void advance() {
        tokenIndex++;
        updateTokens();
    }

    public void updateTokens() {
        if (0 <= tokenIndex && tokenIndex < tokenCount) currentToken = tokens.get(tokenIndex);
    }

    public Token peek(int i) {
        return 0 <= tokenIndex + i && tokenIndex + i < tokenCount ? tokens.get(tokenIndex + i) : null;
    }

    public Token prev(int i) {
        return 0 <= tokenIndex - i && tokenIndex - i < tokenCount ? tokens.get(tokenIndex - i) : null;
    }

    public void reverse(int amount) {
        tokenIndex -= amount;
        updateTokens();
    }

    private String tokenFound() {
        if (currentToken.getValue() != null) return String.valueOf(currentToken.getValue());
        return String.valueOf(currentToken.getType());
    }

    // --- utility methods ---

}
