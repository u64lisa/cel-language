package language.frontend.parser;

import dtool.logger.errors.LanguageException;
import language.frontend.lexer.Lexer;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.cases.ElseCase;
import language.frontend.parser.nodes.definitions.*;
import language.frontend.parser.nodes.expressions.*;
import language.frontend.parser.nodes.extra.CompilerNode;
import language.frontend.parser.nodes.extra.MacroDefinitionNode;
import language.frontend.parser.nodes.operations.BinOpNode;
import language.frontend.parser.nodes.operations.UnaryOpNode;
import language.frontend.parser.nodes.values.*;
import language.frontend.parser.nodes.variables.AttributeAccessNode;
import language.frontend.parser.nodes.variables.TypeDefinitionNode;
import language.frontend.parser.nodes.variables.VarAccessNode;
import language.frontend.parser.results.ParseResult;
import language.frontend.parser.units.Argument;
import language.frontend.parser.units.AttributeGetter;
import language.frontend.parser.units.EnumChild;
import language.frontend.parser.units.TokenMatcher;
import language.utils.Pair;
import language.utils.WrappedCast;

import java.util.*;

/**
 * The type Rewrite parser.
 */
public class RewriteParser extends Parser {

    private final List<String> declarationKeywords = Arrays.asList(
            "static", "private", "public"
    );

    private final List<Token> tokens;
    private Token currentToken;
    private int tokenIndex = -1;
    private int tokenCount;

    /**
     * Instantiates a new Rewrite parser.
     *
     * @param tokens the tokens
     */
    public RewriteParser(List<Token> tokens) {
        this.tokens = tokens;
        this.tokenCount = tokens.size();
        this.push();
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

    /**
     * Statements parse result.
     *
     * @param tks the tks
     * @return the parse result
     */
    public ParseResult<Node> statements(List<TokenMatcher> tks) {
        ParseResult<Node> result = new ParseResult<>();
        List<Node> statements = new ArrayList<>();

        int newlineCount;
        while (currentToken.getType().equals(TokenType.NEW_LINE) ||
                currentToken.getType().equals(TokenType.INVISIBLE_NEW_LINE)) {
            result.registerAdvancement();
            this.push();
        }

        Node statement = result.register(this.parseStatement());

        if (result.getLanguageError() != null)
            return result;

        statements.add(statement);

        boolean moreStatements = true;

        do {
            newlineCount = 0;
            while (currentToken.getType().equals(TokenType.NEW_LINE) ||
                    currentToken.getType().equals(TokenType.INVISIBLE_NEW_LINE)) {
                result.registerAdvancement();
                this.push();
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
                this.pull(result.getToReverseCount());
                moreStatements = false;
                continue;
            }
            statements.add(statement);
        } while (moreStatements);

        this.pull(1);

        if (!currentToken.getType().equals(TokenType.NEW_LINE) &&
                !currentToken.getType().equals(TokenType.INVISIBLE_NEW_LINE)) {
            Node prevStatement = statements.get(statements.size() - 1);

            statements.set(statements.size() - 1, new ReturnNode(prevStatement,
                    prevStatement.getStartPosition(), prevStatement.getEndPosition()));
        }

        this.push();
        return result.success(new BodyNode(statements));
    }

    /**
     * Parse statement parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseStatement() {
        ParseResult<Node> result = new ParseResult<>();
        Position start = currentToken.getStartPosition().copy();

        if (currentToken.getType() == TokenType.LEFT_BRACE) {
            Node statements = result.register(parseBlock());
            if (result.getLanguageError() != null) return result;
            return result.success(new ScopeNode(null, statements));
        }

        if (currentToken.getType() == TokenType.KEYWORD) {

            switch (currentToken.getValue().toString()) {
                case "macro" -> {
                    result.registerAdvancement();
                    this.push();
                    return this.parseMacroDefinition();
                }
                case "for" -> {
                    Node forExpr = result.register(this.parseForLoop());
                    if (result.getLanguageError() != null) return result;
                    return result.success(forExpr);
                }
                case "break" -> {
                    result.registerAdvancement();
                    this.push();
                    return result.success(new BreakNode(start, currentToken.getEndPosition().copy()));
                }
                case "continue" -> {
                    result.registerAdvancement();
                    this.push();
                    return result.success(new ContinueNode(start, currentToken.getEndPosition().copy()));
                }
                case "return" -> {
                    result.registerAdvancement();
                    this.push();
                    Node expr = null;
                    if (currentToken.getType() != TokenType.NEW_LINE && currentToken.getType() != TokenType.INVISIBLE_NEW_LINE) {
                        expr = result.register(this.parseExpression());
                        if (result.getLanguageError() != null) return result;
                    }
                    return result.success(new ReturnNode(expr, start, currentToken.getEndPosition().copy()));
                }
                case "pass" -> {
                    result.registerAdvancement();
                    this.push();
                    return result.success(new PassNode(start, currentToken.getEndPosition().copy()));
                }
                case "assert" -> {
                    result.registerAdvancement();
                    this.push();
                    Node condition = result.register(parseExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(new AssertNode(condition));
                }
                case "typedef" -> {
                    this.push();

                    final Token known = currentToken;
                    this.push(); // known type
                    this.push(); // : symbol
                    final Token newName = currentToken;

                    result.registerAdvancement(); // new name
                    this.push(); // ; symbol

                    return result.success(new TypeDefinitionNode(known, newName));
                }
                case "destructor" -> {
                    Token varTok = result.register(parseIdentifier());
                    if (result.getLanguageError() != null) return result;
                    result.registerAdvancement();
                    this.push();
                    return result.success(new DropNode(varTok));
                }
                case "throw" -> {
                    Node throwNode = result.register(this.parseThrowExpression());
                    if (result.getLanguageError() != null) return result;
                    return result.success(throwNode);
                }
                case "class" -> {
                    Node classDef = result.register(this.parseClassDefinition());
                    if (result.getLanguageError() != null) return result;
                    return result.success(classDef);
                }
                case "compiler" -> {
                    Node compilerDef = result.register(this.parseCompilerDefinition());
                    if (result.getLanguageError() != null) return result;
                    return result.success(compilerDef);
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
                    this.push();
                    result.registerAdvancement();
                    Token importName = currentToken;
                    if (importName.getType() != TokenType.STRING && importName.getType() != TokenType.IDENTIFIER)
                        return unexpected("module name");

                    this.push();
                    result.registerAdvancement();
                    if (currentToken.matches(TokenType.KEYWORD, "as")) {
                        Token identifier = result.register(parseIdentifier("Module name"));
                        if (result.getLanguageError() != null) return result;
                        result.registerAdvancement();
                        this.push();
                        return result.success(new ImportNode(importName, identifier));
                    }
                    return result.success(new ImportNode(importName));
                }

                case "package" -> {
                    this.push();
                    result.registerAdvancement();

                    if (currentToken.getType() != TokenType.STRING && currentToken.getType() != TokenType.IDENTIFIER)
                        return unexpected( "package name");

                    final Token nameToken = currentToken;


                    this.push();
                    result.registerAdvancement();

                    return result.success(new PackageNode(nameToken));

                }

                case "extend" -> {
                    this.push();
                    result.registerAdvancement();

                    if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                        return unexpected("module name");

                    this.push();
                    result.registerAdvancement();
                    return result.success(new ExtendNode(currentToken));
                }
            }

        }

        if (currentToken.getType().equals(TokenType.HASHTAG)) {
            Node useExpr = result.register(this.parseExpressionCall());
            if (result.getLanguageError() != null) return result;
            return result.success(useExpr);
        }

        Node expr = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;
        return result.success(expr);
    }

    private ParseResult<Node> parseCompilerDefinition() {
        ParseResult<Node> result = new ParseResult<>();
        this.push();

        if (this.tokenNotMatching("<"))
            return this.unexpected("<");

        if (!this.expect(TokenType.IDENTIFIER, () -> {}))
            return this.unexpected("identifier");

        Token compilerType = currentToken;
        result.registerAdvancement();
        this.push();

        if (this.tokenNotMatching(">"))
            return this.unexpected(">");

        if (this.tokenNotMatching("export"))
            return this.unexpected("export");

        if (!this.expect(TokenType.IDENTIFIER, () -> {}))
            return unexpected("export name");

        Token exportName = currentToken;
        result.registerAdvancement();
        this.push();

        if (this.tokenNotMatching("{"))
            return this.unexpected("{");

        long start = System.currentTimeMillis();
        final List<Token> stringTokens = new ArrayList<>();
        while (this.currentToken.getType() != TokenType.RIGHT_BRACE) {
            stringTokens.add(currentToken);
            this.push();

            if (System.currentTimeMillis() - start > 5000) {
                return result.failure(
                        new LanguageException("Scope", "failed to collect tokens missing end Bracket"));
            }
        }

        if (this.tokenNotMatching("}"))
            return this.unexpected("}");

        return result.success(new CompilerNode(exportName, compilerType, stringTokens.toArray(new Token[0])));
    }

    /**
     * Parse var parse result.
     *
     * @return the parse result
     */
    public ParseResult<Token> parseVar() {
        ParseResult<Token> result = new ParseResult<>();

        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return unexpected("identifier");

        Token name = currentToken;
        result.registerAdvancement();
        this.push();
        return result.success(name);
    }

    /**
     * Chain expr parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> chainExpr() {
        return binOp(this::parseComparisonExpression, Collections.singletonList(TokenType.COLON));
    }

    /**
     * Parse type token parse result.
     *
     * @return the parse result
     */
    public ParseResult<Token> parseTypeToken() {
        List<String> type = new ArrayList<>();
        Stack<Token> parens = new Stack<>();
        ParseResult<Token> result = new ParseResult<>();
        Position start = currentToken.getStartPosition();
        Position end = start;

        if (!TYPE_TOKENS.contains(currentToken.getType()))
            return unexpected("type");

        ParseType:
        while (!parens.isEmpty() || TYPE_TOKENS.contains(currentToken.getType())) {
            if (currentToken == null)
                return result.failure(LanguageException.invalidSyntax(start, end, "Unmatched parenthesis"));
            switch (currentToken.getType()) {
                case LEFT_ANGLE, LEFT_BRACE, LEFT_BRACKET, LEFT_PAREN -> parens.push(currentToken);
                case RIGHT_ANGLE -> {
                    if (parens.isEmpty())
                        break ParseType;

                    if (!parens.peek().getType().equals(TokenType.LEFT_ANGLE))
                        return unmatched("'>'");

                    parens.pop();
                }
                case ANGLE_ANGLE -> {
                    if (parens.isEmpty())
                        break ParseType;

                    if (!parens.peek().getType().equals(TokenType.LEFT_ANGLE))
                        return unmatched("Unmatched '>'");

                    parens.pop();

                    if (!parens.peek().getType().equals(TokenType.LEFT_ANGLE))
                        return unmatched("Unmatched '>'");

                    parens.pop();
                }
                case RIGHT_PAREN -> {
                    if (parens.isEmpty())
                        break ParseType;

                    if (!parens.peek().getType().equals(TokenType.LEFT_PAREN))
                        return unmatched("')'");

                    parens.pop();
                }
                case RIGHT_BRACKET -> {
                    if (parens.isEmpty())
                        break ParseType;

                    if (!parens.peek().getType().equals(TokenType.LEFT_BRACKET))
                        return unmatched("']'");

                    parens.pop();
                }
                case RIGHT_BRACE -> {
                    if (parens.isEmpty())
                        break ParseType;

                    if (!parens.peek().getType().equals(TokenType.LEFT_BRACE))
                        return unmatched("'}'");

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
            this.push();
        }

        return result.success(new Token(TokenType.TYPE, type, start, end));
    }

    /**
     * Parse expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseExpression() {
        ParseResult<Node> result = new ParseResult<>();
        List<String> type = Collections.singletonList("any");
        if (currentToken.matches(TokenType.KEYWORD, "attribute")) {
            result.registerAdvancement();
            this.push();
            Token name = result.register(parseVar());
            if (result.getLanguageError() != null) return result;

            if (currentToken.getType().equals(TokenType.FAT_ARROW))
                return unexpected("'='");

            if (!currentToken.getType().equals(TokenType.EQUAL))
                return result.success(new AttributeAccessNode(name));

            result.registerAdvancement();
            this.push();

            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            return result.success(new AttributeAssignNode(name, expr));
        } else if (currentToken.getType() == TokenType.KEYWORD &&
                ("constant".equals(currentToken.getValue().toString()) || "var".equals(currentToken.getValue().toString()))) {
            boolean locked = "constant".equalsIgnoreCase(currentToken.getValue().toString());

            result.registerAdvancement();
            this.push();
            if (currentToken.getType() == TokenType.LEFT_BRACE) {
                // Destructors
                List<Token> destructs = new ArrayList<>();
                boolean glob = false;

                result.registerAdvancement();
                this.push();

                // Glob
                if (currentToken.getType() == TokenType.STAR) {
                    result.registerAdvancement();
                    this.push();
                    glob = true;
                } else do {
                    if (currentToken.getType() != TokenType.IDENTIFIER)
                        return unexpected("identifier");

                    destructs.add(currentToken);
                    result.registerAdvancement();
                    this.push();
                } while (currentToken.getType() != TokenType.RIGHT_BRACE);

                if (currentToken.getType() != TokenType.RIGHT_BRACE)
                    return unexpected("'}'");

                result.registerAdvancement();
                this.push();

                if (currentToken.getType() != TokenType.FAT_ARROW)
                    return unexpected("'=>'");

                result.registerAdvancement();
                this.push();

                Node destructed = result.register(parseStatement());
                if (result.getLanguageError() != null) return result;

                if (glob) return result.success(new DestructNode(destructed));
                return result.success(new DestructNode(destructed, destructs));
            }
            Token name = result.register(parseVar());
            if (result.getLanguageError() != null) return result;

            Integer min = null;
            Integer max = null;

            if (currentToken.getType() == TokenType.COMMA) {
                Node nll = new NullNode(new Token(TokenType.IDENTIFIER, "null", currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy()));

                List<Node> varNames = new ArrayList<>(Collections.singletonList(new VarAssignNode(name, nll).setType(type)));
                do {
                    name = result.register(parseIdentifier("Variable name"));
                    if (result.getLanguageError() != null) return result;

                    varNames.add(new VarAssignNode(name, nll).setType(type));
                    result.registerAdvancement();
                    this.push();
                } while (currentToken.getType() == TokenType.COMMA);
                return result.success(new BodyNode(varNames));
            }

            if (currentToken.getType() == TokenType.LEFT_BRACKET) {
                result.registerAdvancement();
                this.push();
                boolean neg = false;
                if (currentToken.getType() == TokenType.MINUS) {
                    neg = true;
                    result.registerAdvancement();
                    this.push();
                }
                if (currentToken.getType() != TokenType.INTEGER)
                    return unexpected("integer");
                min = 0;
                max = ((Double) currentToken.getValue()).intValue() * (neg ? -1 : 1);
                result.registerAdvancement();
                this.push();
                if (currentToken.getType() == TokenType.PIPE) {
                    result.registerAdvancement();
                    this.push();
                    neg = false;
                    if (currentToken.getType() == TokenType.MINUS) {
                        neg = true;
                        result.registerAdvancement();
                        this.push();
                    }
                    if (currentToken.getType() != TokenType.INTEGER)
                        return unexpected("integer");

                    min = max;
                    max = ((Double) currentToken.getValue()).intValue() * (neg ? -1 : 1);
                    result.registerAdvancement();
                    this.push();
                }
                if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                    return unexpected("']'");

                result.registerAdvancement();
                this.push();
            }

            if (currentToken.getType().equals(TokenType.COLON)) {
                this.push();
                result.registerAdvancement();
                Token typeTok = result.register(parseTypeToken());
                if (result.getLanguageError() != null) return result;
                type = WrappedCast.cast(typeTok.getValue());
            }

            if (currentToken.getType().equals(TokenType.FAT_ARROW))
                return unexpected("'='");

            if (!currentToken.getType().equals(TokenType.EQUAL)) {

                return result.success(new VarAssignNode(name, new NullNode(new Token(TokenType.IDENTIFIER, "null",
                        currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy()))).setType(type));
            }

            result.registerAdvancement();
            this.push();
            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;



            return result.success(new VarAssignNode(name, expr, locked).setType(type).setRange(min, max));
        } else if (currentToken.matches(TokenType.KEYWORD, "let")) {
            Token identifier = result.register(parseIdentifier("Variable name"));
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            this.push();

            if (currentToken.getType() != TokenType.EQUAL)
                return unexpected("'='");

            result.registerAdvancement();

            this.push();

            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;

            return result.success(new LetNode(identifier, expr));
        } else if (currentToken.matches(TokenType.KEYWORD, "cal")) {
            result.registerAdvancement();
            this.push();
            Token name = result.register(parseVar());
            if (result.getLanguageError() != null) return result;

            if (!currentToken.getType().equals(TokenType.SKINNY_ARROW))
                return unexpected("weak assignment arrow (->)");

            result.registerAdvancement();
            this.push();
            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            return result.success(new MacroAssignNode(name, expr));
        } else if (currentToken.getType().equals(TokenType.IDENTIFIER)) {
            Token variable = currentToken;
            this.push();
            result.registerAdvancement();
            if (Arrays.asList(TokenType.CARET_EQUALS, TokenType.PLUS_EQUALS, TokenType.START_EQUALS,
                    TokenType.SLASH_EQUALS, TokenType.MINUS_EQUALS).contains(currentToken.getType())) {
                Token operationToken = currentToken;
                this.push();
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
                this.push();

                return result.success(new VarAssignNode(variable, new UnaryOpNode(operationToken.getType(),
                        new VarAccessNode(variable)), false).setDefining(false));
            }
            if (currentToken.getType().equals(TokenType.FAT_ARROW)) {
                result.registerAdvancement();
                this.push();
                Node value = result.register(parseStatement());
                if (result.getLanguageError() != null) return result;

                return result.success(new VarAssignNode(variable, value, false, 1));
            }
            this.pull(1);
        }
        // |Type cast| expr
        if (currentToken.getType() == TokenType.PIPE) {
            result.registerAdvancement();
            this.push();
            Token cast = result.register(parseTypeToken());
            if (result.getLanguageError() != null) return result;
            if (currentToken.getType() != TokenType.PIPE)
                return unexpected("'|'");

            result.registerAdvancement();
            this.push();
            Node expr = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(new CastNode(expr, cast));
        }
        Node node = result.register(binOp(this::parseBitExpression, Collections.singletonList(TokenType.DOT), this::call));

        if (result.getLanguageError() != null) return result;

        return result.success(node);
    }

    /**
     * Parse factor parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseFactor() {
        ParseResult<Node> result = new ParseResult<>();
        Token tok = currentToken;

        if (Arrays.asList(TokenType.PLUS, TokenType.MINUS, TokenType.PLUS_PLUS, TokenType.MINUS_MINUS).contains(tok.getType())) {
            result.registerAdvancement();
            this.push();
            Node factor = result.register(this.parseFactor());
            if (result.getLanguageError() != null) return result;
            return result.success(new UnaryOpNode(tok.getType(), factor));
        }
        return pow();
    }

    /**
     * Parse identifier parse result.
     *
     * @return the parse result
     */
    public ParseResult<Token> parseIdentifier() {
        return parseIdentifier("Identifier");
    }

    /**
     * Parse identifier parse result.
     *
     * @param name the name
     * @return the parse result
     */
    public ParseResult<Token> parseIdentifier(String name) {
        ParseResult<Token> result = new ParseResult<>();
        this.push();
        result.registerAdvancement();
        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return unexpected(name.toLowerCase());


        return result.success(currentToken);
    }

    /**
     * Parse expression call parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseExpressionCall() {
        ParseResult<Node> result = new ParseResult<>();
        Token useToken = result.register(parseIdentifier());

        if (result.getLanguageError() != null) return result;
        this.push();
        result.registerAdvancement();
        List<Token> args = new ArrayList<>();
        while (currentToken.getType().equals(TokenType.IDENTIFIER)) {
            args.add(currentToken);
            result.registerAdvancement();
            this.push();
        }
        return result.success(new UseNode(useToken, args));
    }

    /**
     * Parse bit shift expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseBitShiftExpression() {
        return binOp(this::parseBitWiseExpression,
                Arrays.asList(TokenType.LEFT_TILDE_ARROW, TokenType.TILDE_TILDE, TokenType.RIGHT_TILDE_ARROW), this::parseExpression);
    }

    /**
     * Parse bit wise expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseBitWiseExpression() {
        return binOp(this::parseComplexExpression,
                Arrays.asList(TokenType.TILDE_AMPERSAND, TokenType.TILDE_PIPE, TokenType.TILDE_CARET), this::parseExpression);
    }

    /**
     * Parse complex expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseComplexExpression() {
        ParseResult<Node> result = new ParseResult<>();
        if (currentToken.getType() == TokenType.TILDE || currentToken.getType() == TokenType.DOLLAR) {
            Token opToken = currentToken;
            result.registerAdvancement();
            this.push();

            Node expr = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;

            return result.success(new UnaryOpNode(opToken.getType(), expr));
        }
        return byteExpr();
    }

    /**
     * Byte expr parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> byteExpr() {
        ParseResult<Node> result = new ParseResult<>();
        boolean toBytes = currentToken.getType() == TokenType.AT;
        if (toBytes) {
            result.registerAdvancement();
            this.push();
        }

        Node expr = result.register(this.chainExpr());
        if (result.getLanguageError() != null) return result;

        if (toBytes) return result.success(new BytesNode(expr));
        return result.success(expr);
    }

    /**
     * Atom parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> atom() {
        ParseResult<Node> result = new ParseResult<>();

        Token token = currentToken;

        switch (currentToken.getType()) {
            case INTEGER, FLOAT, DOUBLE, LONG, SHORT, BYTE -> {
                result.registerAdvancement();
                this.push();
                if (currentToken.getType() == TokenType.IDENTIFIER) {
                    if (currentToken.getValue().toString().startsWith("x") && token.getValue().equals(0.0) &&
                            token.getType().equals(TokenType.INTEGER)) {
                        try {
                            Token hexTk = currentToken;
                            result.registerAdvancement();
                            this.push();
                            int hexForm = Integer.parseInt(hexTk.getValue().toString().substring(1), 16);
                            return result.success(new NumberNode(hexForm, hexTk.getStartPosition(), hexTk.getEndPosition()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    Node identifier = new VarAccessNode(currentToken);
                    result.registerAdvancement();
                    this.push();
                    return result.success(new BinOpNode(new NumberNode(token), TokenType.STAR, identifier));
                } else if (currentToken.getType() == TokenType.LEFT_PAREN) {
                    // 3(1 + 2) = 3 * (1 + 2)
                    Node node = new NumberNode(token);
                    while (currentToken.getType() == TokenType.LEFT_PAREN) {
                        result.registerAdvancement();
                        this.push();
                        Node expr = result.register(parseExpression());
                        if (result.getLanguageError() != null) return result;
                        if (currentToken.getType() != TokenType.RIGHT_PAREN)
                            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                                    currentToken.getEndPosition().copy(), "Expected ')' to close expression"));
                        result.registerAdvancement();
                        this.push();
                        node = new BinOpNode(node, TokenType.STAR, expr);
                    }
                    return result.success(node);
                }
                return result.success(new NumberNode(token));
            }

            case KEYWORD -> {
                switch (currentToken.getValue().toString()) {
                    case "attribute" -> {
                        this.push();
                        result.registerAdvancement();
                        Token name = currentToken;

                        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                            return unexpected("identifier");

                        this.push();
                        result.registerAdvancement();
                        return result.success(new AttributeAccessNode(name));
                    }
                    case "scope" -> {
                        result.registerAdvancement();
                        this.push();
                        String name = null;
                        if (currentToken.getType() == TokenType.LEFT_BRACKET) {
                            Token n = result.register(parseIdentifier("Scope"));
                            if (result.getLanguageError() != null) return result;

                            name = n.getValue().toString();

                            result.registerAdvancement();
                            this.push();

                            if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                                return unexpected("']'");

                            result.registerAdvancement();
                            this.push();
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
                    case "inline" -> {
                        Node inlineDefinition = result.register(this.parseInlineFunctionDefinition());
                        if (result.getLanguageError() != null) return result;
                        return result.success(inlineDefinition);
                    }
                    case "null" -> {
                        result.registerAdvancement();
                        this.push();
                        return result.success(new NullNode(currentToken));
                    }
                }
            }
            case SLASH -> {
                this.push();
                result.registerAdvancement();
                Node decorator = result.register(parseFactor());
                if (result.getLanguageError() != null) return result;

                if (currentToken.getType() != TokenType.SLASH)
                    return unexpected("closing slash");

                result.registerAdvancement();
                this.push();
                Node fn = result.register(parseStatement());
                if (result.getLanguageError() != null) return result;
                Token name;
                if (fn.getNodeType() == NodeType.INLINE_DEFINITION)
                    name = ((InlineDeclareNode) fn).name;
                else if (fn.getNodeType() == NodeType.CLASS_DEFINITION)
                    name = ((ClassDefNode) fn).className;
                else if (fn.getNodeType() == NodeType.DECORATOR)
                    name = ((DecoratorNode) fn).name;
                else
                    return result.failure(LanguageException.invalidSyntax(fn.getStartPosition().copy(),
                            fn.getEndPosition().copy(), "Object is not deco-ratable"));

                return result.success(new DecoratorNode(decorator, fn, name));
            }

            case QUESTION_MARK -> {
                Node queryExpr = result.register(this.parseQueryExpression());
                if (result.getLanguageError() != null) return result;
                return result.success(queryExpr);
            }
            case LEFT_BRACKET -> {
                Node listExpr = result.register(this.parseListExpression());
                if (result.getLanguageError() != null) return result;
                return result.success(listExpr);
            }
            case LEFT_BRACE -> {
                Node dictExpr = result.register(this.parseDictionaryExpression());
                if (result.getLanguageError() != null) return result;
                return result.success(dictExpr);
            }
            case LEFT_PAREN -> {
                result.registerAdvancement();
                this.push();
                Node expr = result.register(this.parseExpression());
                if (result.getLanguageError() != null) return result;
                if (currentToken.getType().equals(TokenType.RIGHT_PAREN)) {
                    result.registerAdvancement();
                    this.push();
                    return result.success(expr);
                } else
                    return unexpected("')'");
            }
            case STRING -> {
                final Pair<String, Boolean> element = WrappedCast.cast(token.getValue());
                if (element.getLast()) {
                    Node val = result.register(formatStringExpr());
                    if (result.getLanguageError() != null) return result;
                    return result.success(val);
                }
                result.registerAdvancement();
                this.push();
                return result.success(new StringNode(token));
            }
            case BOOLEAN -> {
                result.registerAdvancement();
                this.push();
                return result.success(new BooleanNode(token));
            }
            case IDENTIFIER -> {
                result.registerAdvancement();
                this.push();
                if (currentToken.getType().equals(TokenType.EQUAL))
                    return unexpected("'=>'");

                return result.success(new VarAccessNode(token));
            }

        }

        return unexpected(String.format("number, identifier, '+', '-', or '('. Found %s", tokenFound()));
    }

    /**
     * Parse reference expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseReferenceExpression() {
        Token prefixToken;
        if (currentToken.getType() == TokenType.STAR || currentToken.getType() == TokenType.AMPERSAND) {
            ParseResult<Node> result = new ParseResult<>();

            prefixToken = currentToken;
            result.registerAdvancement();
            this.push();

            Node expr = result.register(parseReferenceExpression());
            if (result.getLanguageError() != null) return result;

            if (prefixToken.getType() == TokenType.STAR)
                return result.success(new DerefNode(expr));
            else return result.success(new RefNode(expr));
        }

        return binOp(this::call, Collections.singletonList(TokenType.LEFT_BRACKET), this::parseExpression);
    }

    /**
     * Format string expr parse result.
     *
     * @return the parse result
     */
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
                node = new BinOpNode(node, addToken, new StringNode(new Token(TokenType.STRING,
                        new Pair<>(sb.toString(), false), tok.getStartPosition(), tok.getEndPosition())));
                sb = new StringBuilder();
                StringBuilder expr = new StringBuilder();
                i += 2;
                while (i < val.getFirst().length() && val.getFirst().charAt(i) != '}') {
                    current = val.getFirst().charAt(i);
                    expr.append(current);
                    i++;
                }

                if (i >= val.getFirst().length())
                    return result.failure(LanguageException.invalidSyntax(tok.getStartPosition(),
                            tok.getEndPosition(), "Unmatched bracket"));

                Lexer lexer = new Lexer("<string>", expr.toString());
                List<Token> expressionStatements = lexer.lex();

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
        this.push();
        return result.success(new BinOpNode(node, addToken, new StringNode(new Token(TokenType.STRING,
                new Pair<>(sb.toString(), false), tok.getStartPosition(), tok.getEndPosition()))));
    }

    /**
     * Parse throw expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseThrowExpression() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "throw"))
            return unexpected("'throw'");

        result.registerAdvancement();
        this.push();

        Node first = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;

        if (currentToken.getType() == TokenType.COMMA) {
            result.registerAdvancement();
            this.push();

            Node second = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;

            return result.success(new ThrowNode(first, second));
        }

        return result.success(new ThrowNode(
                new StringNode(
                        new Token(TokenType.STRING,
                                new Pair<>("Thrown", false),
                                first.getStartPosition(), first.getEndPosition())), first));
    }

    // MACRO SECTION START --------------------------------------------------------------------------------------------------

    /**
     * Parse macro definition parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseMacroDefinition() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return unexpected("macro name");

        String macroName = currentToken.getValue().toString();
        this.push();

        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return unexpected("'('");

        result.registerAdvancement();
        this.push();

        List<Token> body = readMacroSection(TokenType.RIGHT_PAREN);

        return result.success(new MacroDefinitionNode(macroName, body));
    }

    /**
     * Read macro section list.
     *
     * @param closing the closing
     * @return the list
     */
    public List<Token> readMacroSection(final TokenType closing) {
        List<Token> tokens = new ArrayList<>();

        while (currentToken.getType() != closing) {
            switch (currentToken.getType()) {

                case LEFT_PAREN -> {
                    tokens.add(currentToken);
                    readMacroScopeHead(TokenType.LEFT_PAREN);
                    tokens.addAll(readMacroSection(TokenType.RIGHT_PAREN));
                }
                case LEFT_BRACE -> {
                    tokens.add(currentToken);
                    readMacroScopeHead(TokenType.LEFT_BRACE);
                    tokens.addAll(readMacroSection(TokenType.RIGHT_BRACE));
                }
                case LEFT_BRACKET -> {
                    tokens.add(currentToken);
                    readMacroScopeHead(TokenType.LEFT_BRACKET);
                    tokens.addAll(readMacroSection(TokenType.RIGHT_BRACKET));
                }

                default -> {
                    tokens.add(currentToken);
                    this.push();
                }

            }

        }
        tokens.add(currentToken);
        this.push();

        return tokens;
    }

    /**
     * Read macro scope head.
     *
     * @param type the type
     */
    public void readMacroScopeHead(TokenType type) {
        if (currentToken.getType().equals(type)) {
            this.push();
            return;
        }

        throw new ParseException(LanguageException.expected(currentToken.getStartPosition().copy(),
                currentToken.getEndPosition().copy(), "Expected " + type));
    }

    /**
     * Parse structure definition parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseStructureDefinition() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "structure"))
            return unexpected("'struct'");

        List<AttributeDeclareNode> childrenDeclarations = new ArrayList<>();
        List<Token> children = new ArrayList<>();
        List<Token> types = new ArrayList<>();
        List<Node> assignment = new ArrayList<>();

        Token identifier = result.register(parseIdentifier("Struct"));
        if (result.getLanguageError() != null) return result;
        result.registerAdvancement();
        this.push();

        if (currentToken.getType() != TokenType.LEFT_BRACE)
            return unexpected("'{'");

        do {
            result.register(parseIdentifier("Attribute name"));
            if (result.getLanguageError() != null) return result;

            children.add(currentToken);
            childrenDeclarations.add(new AttributeDeclareNode(currentToken));
            types.add(new Token(TokenType.TYPE, Collections.singletonList("any"), currentToken.getStartPosition(), currentToken.getEndPosition()));
            assignment.add(new AttributeAssignNode(currentToken, new VarAccessNode(currentToken)));

            result.registerAdvancement();
            this.push();
        } while (currentToken.getType() == TokenType.COMMA);

        Position end = currentToken.getEndPosition().copy();

        if (currentToken.getType() != TokenType.RIGHT_BRACE)
            return unexpected("'}'");

        endLine(1);
        result.registerAdvancement();
        this.push();

        return result.success(new ClassDefNode(identifier, childrenDeclarations, children, types, new BodyNode(assignment),
                new ArrayList<>(), end, new ArrayList<>(), 0, null, new ArrayList<>(), null, null));

    }

    /**
     * Parse enum expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseEnumExpression() {
        ParseResult<Node> result = new ParseResult<>();

        List<EnumChild> children = new ArrayList<>();
        Token name;

        if (!currentToken.matches(TokenType.KEYWORD, "enum"))
            return unexpected("'enum'");

        result.registerAdvancement();
        this.push();

        boolean pub = currentToken.matches(TokenType.KEYWORD, "public");
        if (pub) {
            result.registerAdvancement();
            this.push();
        }

        if (currentToken.getType() != TokenType.IDENTIFIER)
            return unexpected("identifier");

        name = currentToken;
        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return unexpected("'{'");

        result.registerAdvancement();
        this.push();

        while (currentToken.getType() == TokenType.IDENTIFIER) {
            Token token = currentToken;

            result.registerAdvancement();
            this.push();

            List<String> generics = new ArrayList<>();
            if (currentToken.getType() == TokenType.LEFT_PAREN) {
                do {
                    Token identifier = result.register(parseIdentifier("Generic type"));
                    if (result.getLanguageError() != null) return result;
                    result.registerAdvancement();
                    this.push();
                    generics.add(identifier.getValue().toString());
                } while (currentToken.getType() == TokenType.COMMA);

                if (currentToken.getType() != TokenType.RIGHT_PAREN)
                    return unexpected("')'");

                result.registerAdvancement();
                this.push();
            }

            List<String> params = new ArrayList<>();
            List<List<String>> types = new ArrayList<>();
            if (currentToken.getType() == TokenType.LEFT_BRACE) {
                do {
                    Token tok = result.register(parseIdentifier("Parameter"));
                    if (result.getLanguageError() != null) return result;
                    params.add((String) tok.getValue());
                    result.registerAdvancement();
                    this.push();

                    if (currentToken.getType() == TokenType.COLON) {
                        result.registerAdvancement();
                        this.push();
                        tok = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        types.add(WrappedCast.cast(tok.getValue()));
                    } else {
                        types.add(Collections.singletonList("any"));
                    }

                } while (currentToken.getType() == TokenType.COMMA);

                if (currentToken.getType() != TokenType.RIGHT_BRACE)
                    return unexpected("'}'");

                result.registerAdvancement();
                this.push();
            }

            if (currentToken.getType() != TokenType.COMMA)
                return unexpected("comma");

            result.registerAdvancement();
            this.push();
            children.add(new EnumChild(token, params, types, generics));
        }

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return unexpected("'}'");

        endLine(1);
        result.registerAdvancement();
        this.push();

        return result.success(new EnumNode(name, children, pub));
    }

    /**
     * Parse switch expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseSwitchExpression() {
        ParseResult<Node> result = new ParseResult<>();

        ElseCase elseCase = null;
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TokenType.KEYWORD, "switch"))
            return unexpected("switch");

        result.registerAdvancement();
        this.push();

        Node ref;
        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return unexpected("'('");

        result.registerAdvancement();
        this.push();

        ref = result.register(parseExpression());
        if (result.getLanguageError() != null) return result;

        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return unexpected("')'");

        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return unexpected("'{'");

        result.registerAdvancement();
        this.push();

        boolean def;
        Node condition, body;
        while (currentToken.matches(TokenType.KEYWORD, "case") || currentToken.matches(TokenType.KEYWORD, "default")) {
            def = currentToken.matches(TokenType.KEYWORD, "default");
            result.registerAdvancement();
            this.push();

            if (!def) {
                condition = result.register(parseComparisonExpression());
                if (result.getLanguageError() != null) return result;
            } else condition = null;

            if (currentToken.getType() != TokenType.COLON)
                return unexpected("':'");

            result.registerAdvancement();
            this.push();

            body = result.register(statements(Arrays.asList(new TokenMatcher(TokenType.RIGHT_BRACE, null),
                    new TokenMatcher(TokenType.KEYWORD, "case"), new TokenMatcher(TokenType.KEYWORD, "default"))));
            if (result.getLanguageError() != null) return result;

            if (def) elseCase = new ElseCase(body, false);
            else cases.add(new Case(condition, body, false));
        }

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return unexpected("'}'");

        endLine(1);
        result.registerAdvancement();
        this.push();

        Node switchNode = new SwitchNode(ref, cases, elseCase, false);
        return result.success(switchNode);
    }

    /**
     * Expect semicolon parse result.
     *
     * @return the parse result
     */
    public ParseResult<Void> expectSemicolon() {
        if (currentToken.getType() != TokenType.INVISIBLE_NEW_LINE && currentToken.getType() != TokenType.NEW_LINE)
            return unexpected("';'");

        return new ParseResult<>();
    }

    /**
     * Pattern expr parse result.
     *
     * @param expr the expr
     * @return the parse result
     */
    public ParseResult<Node> patternExpr(Node expr) {
        ParseResult<Node> result = new ParseResult<>();
        HashMap<Token, Node> patterns = new HashMap<>();

        if (currentToken.getType() != TokenType.LEFT_PAREN)
            return unexpected("'('");

        if (peek(1).getType() == TokenType.IDENTIFIER) do {
            Token identifier = result.register(parseIdentifier());
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            this.push();

            if (currentToken.getType() != TokenType.COLON) {
                patterns.put(identifier, new VarAccessNode(identifier));
                continue;
            }
            result.registerAdvancement();
            this.push();

            Node pattern = result.register(this.parseExpression());
            if (result.getLanguageError() != null) return result;

            patterns.put(identifier, pattern);
        } while (currentToken.getType() == TokenType.COMMA);
        else {
            result.registerAdvancement();
            this.push();
        }

        if (currentToken.getType() != TokenType.RIGHT_PAREN)
            return unexpected("')'");

        result.registerAdvancement();
        this.push();

        return result.success(new PatternNode(expr, patterns));
    }

    /**
     * Parse matching expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseMatchingExpression() {
        ParseResult<Node> result = new ParseResult<>();

        ElseCase elseCase = null;
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TokenType.KEYWORD, "match"))
            return unexpected("match");

        result.registerAdvancement();
        this.push();

        Node ref;
        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return unexpected("'('");

        result.registerAdvancement();
        this.push();
        ref = result.register(parseExpression());
        if (result.getLanguageError() != null) return result;
        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return unexpected("')'");

        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return unexpected("'{'");

        result.registerAdvancement();
        this.push();

        Node body;
        boolean pat, def;
        while (currentToken.getType() != TokenType.RIGHT_BRACE) {
            pat = !currentToken.matches(TokenType.KEYWORD, "case") && !currentToken.matches(TokenType.KEYWORD, "default");
            def = currentToken.matches(TokenType.KEYWORD, "default");

            List<Node> conditions = new ArrayList<>();
            this.pull(1);
            do {
                result.registerAdvancement();
                this.push();
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
                    this.push();
                    if (!def) {
                        condition = result.register(parseStatement());
                        if (result.getLanguageError() != null) return result;
                    } else condition = null;
                }
                if (condition != null) conditions.add(condition);
            } while (currentToken.getType() == TokenType.COMMA);

            if (currentToken.getType() != TokenType.SKINNY_ARROW)
                return unexpected("'->'");

            result.registerAdvancement();
            this.push();

            body = result.register(parseStatement());
            if (result.getLanguageError() != null) return result;

            if (def) elseCase = new ElseCase(body, true);
            else for (Node condition : conditions)
                cases.add(new Case(condition, body, true));
            result.register(expectSemicolon());
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            this.push();
        }

        result.registerAdvancement();
        this.push();

        Node switchNode = new SwitchNode(ref, cases, elseCase, true);
        return result.success(switchNode);
    }

    /**
     * Call parse result.
     *
     * @return the parse result
     */
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
                            this.push();

                            if (currentToken.getType() == TokenType.DOT_DOT) {
                                result.registerAdvancement();
                                this.push();
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
                        this.push();
                    }

                    if (currentToken.getType() == TokenType.BACK_SLASH) {
                        Token vk;
                        Node val;
                        do {
                            vk = result.register(parseIdentifier());
                            if (result.getLanguageError() != null) return result;
                            result.registerAdvancement();
                            this.push();

                            if (currentToken.getType() != TokenType.COLON)
                                return unexpected("':'");
                            result.registerAdvancement();
                            this.push();

                            val = result.register(this.parseExpression());
                            if (result.getLanguageError() != null) return result;

                            keywordArguments.put(vk.getValue().toString(), val);
                        } while (currentToken.getType() == TokenType.COMMA);
                    }

                    if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
                        return unexpected("',' or ')'");

                } else {
                    result.registerAdvancement();
                    this.push();
                }
                result.registerAdvancement();
                this.push();
                if (currentToken.getType() == TokenType.LEFT_ANGLE && TYPE_TOKENS.contains(peek(1).getType())) {
                    int startIndex = tokenIndex;
                    result.registerAdvancement();
                    this.push();

                    ParseResult<Token> typeToken = parseTypeToken();
                    if (typeToken.getLanguageError() != null) return result.failure(typeToken.getLanguageError());
                    generics.add(result.register(typeToken));

                    while (currentToken.getType() == TokenType.COMMA) {
                        result.registerAdvancement();
                        this.push();

                        typeToken = parseTypeToken();
                        if (typeToken.getLanguageError() != null) return result.failure(typeToken.getLanguageError());
                        generics.add(result.register(typeToken));
                    }
                    if (currentToken.getType() != TokenType.RIGHT_ANGLE) {
                        generics = new ArrayList<>();
                        tokenIndex = startIndex;
                        updateTokens();
                    } else {
                        result.registerAdvancement();
                        this.push();
                    }
                }

                node = new CallNode(node, argumentNodes, generics, keywordArguments);
            } else {
                Token tok = result.register(parseIdentifier());

                if (result.getLanguageError() != null) return result;
                this.push();
                result.registerAdvancement();
                node = new ClassAccessNode(node, tok);
            }
        }
        return result.success(node);
    }

    /**
     * Pow parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> pow() {
        return binOp(this::refOp, Arrays.asList(TokenType.CARET, TokenType.PERCENT), this::parseFactor);
    }

    /**
     * Ref op parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> refOp() {
        return binOp(this::refSugars, Collections.singletonList(TokenType.FAT_ARROW));
    }

    /**
     * Ref sugars parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> refSugars() {
        ParseResult<Node> result = new ParseResult<>();

        Node expr = result.register(this.parseReferenceExpression());
        if (result.getLanguageError() != null) return result;

        while (Arrays.asList(
                TokenType.PLUS_EQUALS, TokenType.MINUS_EQUALS,
                TokenType.START_EQUALS, TokenType.SLASH_EQUALS,
                TokenType.CARET_EQUALS).contains(currentToken.getType()) ||
                Arrays.asList(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)
                        .contains(currentToken.getType())) {

            if (Arrays.asList(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)
                    .contains(currentToken.getType())) {

                expr = new BinOpNode(expr, TokenType.FAT_ARROW, new UnaryOpNode(currentToken.getType(), expr));

                result.registerAdvancement();
                this.push();
            } else if (Arrays.asList(
                    TokenType.PLUS_EQUALS, TokenType.MINUS_EQUALS,
                    TokenType.START_EQUALS, TokenType.SLASH_EQUALS,
                    TokenType.CARET_EQUALS
            ).contains(currentToken.getType())) {
                TokenType opTok = switch (currentToken.getType()) {
                    case CARET_EQUALS -> TokenType.CARET;
                    case START_EQUALS -> TokenType.STAR;
                    case SLASH_EQUALS -> TokenType.SLASH;
                    case PLUS_EQUALS -> TokenType.PLUS;
                    case MINUS_EQUALS -> TokenType.MINUS;
                    default -> null;
                };
                result.registerAdvancement();
                this.push();

                Node right = result.register(this.parseExpression());
                if (result.getLanguageError() != null) return result;

                expr = new BinOpNode(expr, TokenType.FAT_ARROW, new BinOpNode(expr, opTok, right));
            }
        }

        return result.success(expr);
    }

    /**
     * Parse term parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseTerm() {
        return binOp(this::parseFactor, Arrays.asList(TokenType.STAR, TokenType.SLASH));
    }

    /**
     * Parse attribute parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseAttribute() {
        return binOp(this::parseTerm, Arrays.asList(TokenType.PLUS, TokenType.MINUS));
    }

    /**
     * Parse comparison expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseComparisonExpression() {
        ParseResult<Node> result = new ParseResult<>();
        if (currentToken.getType().equals(TokenType.BANG)) {
            Token operationToken = currentToken;
            result.registerAdvancement();
            this.push();

            Node node = result.register(parseComparisonExpression());
            if (result.getLanguageError() != null) return result;
            return result.success(new UnaryOpNode(operationToken.getType(), node));
        }
        Node node = result.register(binOp(this::parseAttribute, Arrays.asList(TokenType.EQUAL_EQUAL,
                TokenType.BANG_EQUAL, TokenType.LEFT_ANGLE, TokenType.RIGHT_ANGLE, TokenType.LESS_EQUALS, TokenType.GREATER_EQUALS)));

        if (result.getLanguageError() != null) return result;
        return result.success(node);
    }

    /**
     * Parse bit expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseBitExpression() {
        ParseResult<Node> result = new ParseResult<>();
        Node node = result.register(binOp(this::parseBitShiftExpression, Arrays.asList(TokenType.AMPERSAND, TokenType.PIPE)));
        if (result.getLanguageError() != null) return result;
        return result.success(node);
    }

    /**
     * Bin op parse result.
     *
     * @param leftParse the left parse
     * @param ops       the ops
     * @return the parse result
     */
    public ParseResult<Node> binOp(NodeCallback leftParse, List<TokenType> ops) {
        return binOp(leftParse, ops, null);
    }

    /**
     * Bin op parse result.
     *
     * @param leftParse  the left parse
     * @param ops        the ops
     * @param rightParse the right parse
     * @return the parse result
     */
    public ParseResult<Node> binOp(NodeCallback leftParse, List<TokenType> ops, NodeCallback rightParse) {
        ParseResult<Node> result = new ParseResult<>();
        if (rightParse == null) rightParse = leftParse;
        Node right;
        Node left;
        left = result.register(leftParse.call());
        if (result.getLanguageError() != null) return result;

        while (ops.contains(currentToken.getType())) {
            TokenType operationToken = currentToken.getType();
            result.registerAdvancement();
            this.push();
            right = result.register(rightParse.call());
            if (result.getLanguageError() != null) return result;
            if (operationToken == TokenType.LEFT_BRACKET) {

                if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                    return unexpected("closing bracket (']')");

                this.push();
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

    /**
     * Parse arguments parse result.
     *
     * @return the parse result
     */
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
            this.push(); // (
            this.push(); // )
            // ignore
        } else if (currentToken.getType().equals(TokenType.LEFT_PAREN)) {
            if (peek(1) != null && peek(1).getType() != TokenType.BACK_SLASH && peek(1).getType() != TokenType.RIGHT_ANGLE) {
                boolean args;
                do {
                    result.registerAdvancement();
                    this.push();

                    args = currentToken.getType() == TokenType.DOT_DOT;
                    if (args) {
                        result.registerAdvancement();
                        this.push();
                    }

                    if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                        return unexpected("identifier");


                    if (args) {
                        argumentName = currentToken.getValue().toString();
                        result.registerAdvancement();
                        this.push();
                        break;
                    }

                    argNameTokens.add(currentToken);
                    result.registerAdvancement();
                    this.push();


                    if (currentToken.getType().equals(TokenType.LEFT_ANGLE)) {
                        Token typeToken = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        List<String> type = WrappedCast.cast(typeToken.getValue());

                        // Expect :
                        if (currentToken.getType() != TokenType.COLON)
                            return unexpected("':' after type annotation");

                        result.registerAdvancement();
                        this.push();
                        Token calledTypeTok = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        List<String> calledType = WrappedCast.cast(calledTypeTok.getValue());
                        calledType.addAll(type);
                        argTypeTokens.add(calledTypeTok);
                    } else if (currentToken.getType().equals(TokenType.COLON)) {
                        result.registerAdvancement();
                        this.push();

                        Token typeToken = result.register(parseTypeToken());
                        if (result.getLanguageError() != null) return result;
                        argTypeTokens.add(typeToken);
                    } else
                        argTypeTokens.add(new Token(TokenType.TYPE, Collections.singletonList("any"),
                                currentToken.getStartPosition(), currentToken.getEndPosition()));
                    if (currentToken.getType().equals(TokenType.EQUAL)) {
                        result.registerAdvancement();
                        this.push();

                        Node val = result.register(parseAttribute());
                        if (result.getLanguageError() != null) return result;

                        defaults.add(val);
                        defaultCount++;
                        optionals = true;

                    } else if (optionals)
                        return unexpected("default value");

                } while (currentToken.getType().equals(TokenType.COMMA));
            } else {
                result.registerAdvancement();
                this.push();
            }

            if (currentToken.getType() == TokenType.BACK_SLASH) {
                Token kw = result.register(parseIdentifier("Parameter"));
                if (result.getLanguageError() != null) return result;
                result.registerAdvancement();
                this.push();
                keywordArgument = kw.getValue().toString();
            }

            if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
                return unexpected("')'");

            this.push();
            result.registerAdvancement();
        }

        List<Token> generics = new ArrayList<>();
        if (currentToken.getType().equals(TokenType.LEFT_ANGLE)) {
            result.registerAdvancement();
            this.push();
            do {
                if (currentToken.getType() == TokenType.COMMA) {
                    result.registerAdvancement();
                    this.push();
                }

                if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                    return unexpected("type");


                generics.add(currentToken);
                result.registerAdvancement();
                this.push();
            } while (currentToken.getType() == TokenType.COMMA);
            if (!currentToken.getType().equals(TokenType.RIGHT_ANGLE))
                return unexpected("')'");
            result.registerAdvancement();
            this.push();
        }

        return result.success(new Argument(argNameTokens, argTypeTokens, defaults, defaultCount, generics, argumentName, keywordArgument));
    }

    /**
     * End line.
     *
     * @param offset the offset
     */
    public void endLine(int offset) {
        tokens.add(tokenIndex + offset, new Token(TokenType.INVISIBLE_NEW_LINE,
                currentToken.getStartPosition().copy(), currentToken.getStartPosition().copy()));
        tokenCount++;
    }

    /**
     * Parse block parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseBlock() {
        return parseBlock(true);
    }

    /**
     * Parse block parse result.
     *
     * @param vLine the v line
     * @return the parse result
     */
    public ParseResult<Node> parseBlock(boolean vLine) {
        ParseResult<Node> result = new ParseResult<>();
        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return unexpected("'{'");

        result.registerAdvancement();
        this.push();

        Node statements = result.register(this.statements(Collections.singletonList(new TokenMatcher(TokenType.RIGHT_BRACE, null))));
        if (result.getLanguageError() != null) return result;

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return unexpected("'}'");

        if (vLine) endLine(1);
        result.registerAdvancement();
        this.push();

        return result.success(statements);
    }

    // EXPRESSIONS

    // If Parts

    /**
     * Parse if expression parse result.
     *
     * @return the parse result
     */
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

    /**
     * Parse if cases parse result.
     *
     * @param caseKeyword the case keyword
     * @param parenthesis the parenthesis
     * @return the parse result
     */
    public ParseResult<Pair<List<Case>, ElseCase>> parseIfCases(String caseKeyword, boolean parenthesis) {
        ParseResult<Pair<List<Case>, ElseCase>> result = new ParseResult<>();
        List<Case> cases = new ArrayList<>();

        if (!currentToken.matches(TokenType.KEYWORD, caseKeyword))
            return unexpected(caseKeyword);

        result.registerAdvancement();
        this.push();

        if (parenthesis) {
            if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
                return unexpected("'('");
            result.registerAdvancement();
            this.push();
        }
        Node condition = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;

        if (parenthesis) {
            if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
                return unexpected("')'");
            result.registerAdvancement();
            this.push();
        }

        Node statements;
        if (currentToken.getType().equals(TokenType.LEFT_BRACE)) statements = result.register(this.parseBlock(false));
        else {
            statements = result.register(this.parseStatement());
            result.register(expectSemicolon());
            if (result.getLanguageError() != null) return result;
            result.registerAdvancement();
            this.push();
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

    /**
     * Parse else if statement parse result.
     *
     * @param parenthesis the parenthesis
     * @return the parse result
     */
    public ParseResult<Pair<List<Case>, ElseCase>> parseElseIfStatement(boolean parenthesis) {
        ParseResult<Pair<List<Case>, ElseCase>> result = new ParseResult<>();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase;

        if (currentToken.matches(TokenType.KEYWORD, "elseIf")) {
            Pair<List<Case>, ElseCase> allCases = result.register(parseIfCases("elseIf", parenthesis));
            if (result.getLanguageError() != null) return result;
            cases = allCases.getFirst();
            elseCase = allCases.getLast();
        } else {
            elseCase = result.register(this.elseExpr());
            if (result.getLanguageError() != null) return result;
        }
        return result.success(new Pair<>(cases, elseCase));

    }

    /**
     * Else expr parse result.
     *
     * @return the parse result
     */
    public ParseResult<ElseCase> elseExpr() {
        ParseResult<ElseCase> result = new ParseResult<>();
        ElseCase elseCase = null;

        if (currentToken.matches(TokenType.KEYWORD, "else")) {
            result.registerAdvancement();
            this.push();

            Node statements;
            if (currentToken.getType().equals(TokenType.LEFT_BRACE)) statements = result.register(this.parseBlock());
            else {
                statements = result.register(this.parseStatement());
                result.register(expectSemicolon());
                if (result.getLanguageError() != null) return result;
                result.registerAdvancement();
                this.push();
            }
            if (result.getLanguageError() != null) return result;
            elseCase = new ElseCase(statements, false);
        }

        return result.success(elseCase);
    }

    // Query

    /**
     * Kv parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> kv() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.getType().equals(TokenType.COLON))
            return unexpected("':'");

        result.registerAdvancement();
        this.push();
        Node expr = result.register(parseExpression());
        if (result.getLanguageError() != null) return result;
        return result.success(expr);
    }

    /**
     * Parse query expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseQueryExpression() {
        ParseResult<Node> result = new ParseResult<>();
        List<Case> cases = new ArrayList<>();
        ElseCase elseCase = null;

        NodeCallback statement = () -> {
            result.registerAdvancement();
            this.push();
            Node condition = result.register(parseComparisonExpression());
            if (result.getLanguageError() != null) return result;
            Node expr = result.register(this.kv());
            if (result.getLanguageError() != null) return result;
            cases.add(new Case(condition, expr, true));
            return null;
        };

        if (!currentToken.getType().equals(TokenType.QUESTION_MARK))
            return unexpected("'?'");

        ParseResult<Node> parseResult;
        parseResult = statement.call();
        if (parseResult != null) return parseResult;

        while (currentToken.getType().equals(TokenType.DOLLAR)) {
            parseResult = statement.call();
            if (parseResult != null) return parseResult;
        }

        if (currentToken.getType().equals(TokenType.DOLLAR_UNDER_SCORE)) {
            result.registerAdvancement();
            this.push();

            if (!currentToken.getType().equals(TokenType.COLON))
                return unexpected("':'");

            result.registerAdvancement();
            this.push();

            Node expr = result.register(this.parseStatement());
            if (result.getLanguageError() != null) return result;
            elseCase = new ElseCase(expr, true);
        }
        return result.success(new QueryNode(cases, elseCase));
    }

    // Loops

    /**
     * Parse for loop parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseForLoop() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "for"))
            return unexpected("'for'");

        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return unexpected("'('");
        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return unexpected("identifier");


        Token varName = currentToken;
        result.registerAdvancement();
        this.push();

        boolean iterating = currentToken.getType().equals(TokenType.LEFT_ARROW);
        if (!currentToken.getType().equals(TokenType.SKINNY_ARROW) && !currentToken.getType().equals(TokenType.LEFT_ARROW))
            return unexpected("weak assignment or iterator ('->', '<-')");
        result.registerAdvancement();
        this.push();

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
                    this.push();
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
            return unexpected("':'");
        result.registerAdvancement();
        this.push();

        Node end = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;

        Node step;
        if (currentToken.getType().equals(TokenType.ANGLE_ANGLE)) {
            result.registerAdvancement();
            this.push();
            step = result.register(this.parseExpression());
        } else step = null;

        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return unexpected("')'");
        result.registerAdvancement();
        this.push();

        Node body;
        switch (currentToken.getType()) {
            case LEFT_BRACE -> {
                body = result.register(this.parseBlock());
                if (result.getLanguageError() != null) return result;
                return result.success(new ForNode(varName, start, end, step, body, true));
            }
            case FAT_ARROW -> {
                result.registerAdvancement();
                this.push();
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

    /**
     * Gets closing.
     *
     * @return the closing
     */
    public ParseResult<Node> getClosing() {
        ParseResult<Node> result = new ParseResult<>();
        Node condition = result.register(this.parseExpression());
        if (result.getLanguageError() != null) return result;
        if (!currentToken.getType().equals(TokenType.RIGHT_PAREN))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected ')'"));
        result.registerAdvancement();
        this.push();
        return result.success(condition);
    }

    /**
     * Parse while loop parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseWhileLoop() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "while"))
            return unexpected("'while'");
        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.LEFT_PAREN))
            return unexpected("'('");
        result.registerAdvancement();
        this.push();

        Node condition = result.register(getClosing());
        if (result.getLanguageError() != null) return result;

        return result.success(condition);
    }

    /**
     * Parse do expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseDoExpression() {
        ParseResult<Node> result = new ParseResult<>();

        if (!currentToken.matches(TokenType.KEYWORD, "do"))
            return unexpected("'do'");

        result.registerAdvancement();
        this.push();

        Node body;
        boolean bracket = currentToken.getType() == TokenType.LEFT_BRACE;
        switch (currentToken.getType()) {
            case FAT_ARROW -> {
                result.registerAdvancement();
                this.push();
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

    /**
     * Parse while expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseWhileExpression() {
        ParseResult<Node> result = new ParseResult<>();

        Node condition;
        if (currentToken.matches(TokenType.KEYWORD, "loop")) {
            Token loopTok = currentToken;
            result.registerAdvancement();
            this.push();
            condition = new BooleanNode(new Token(TokenType.BOOLEAN, true, loopTok.getStartPosition(), loopTok.getEndPosition()));
        } else {
            condition = result.register(parseWhileLoop());

            if (result.getLanguageError() != null) return result;
        }
        Node body;
        switch (currentToken.getType()) {
            case FAT_ARROW -> {
                result.registerAdvancement();
                this.push();
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

    /**
     * Parse list expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseListExpression() {
        ParseResult<Node> result = new ParseResult<>();
        List<Node> elementNodes = new ArrayList<>();
        Position start = currentToken.getStartPosition().copy();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACKET))
            return unexpected("'['");
        result.registerAdvancement();
        this.push();

        if (!currentToken.getType().equals(TokenType.RIGHT_BRACKET)) {
            elementNodes.add(result.register(this.parseExpression()));
            if (result.getLanguageError() != null) return result;

            while (currentToken.getType().equals(TokenType.COMMA)) {
                result.registerAdvancement();
                this.push();
                elementNodes.add(result.register(this.parseExpression()));
                if (result.getLanguageError() != null) return result;
            }
            if (!currentToken.getType().equals(TokenType.RIGHT_BRACKET))
                return unexpected("']'");
        }
        result.registerAdvancement();
        this.push();
        return result.success(new ListNode(elementNodes, start, currentToken.getEndPosition().copy()));
    }

    /**
     * Parse dictionary expression parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseDictionaryExpression() {
        ParseResult<Node> result = new ParseResult<>();
        Map<Node, Node> dict = new HashMap<>();
        Position start = currentToken.getStartPosition().copy();

        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return unexpected("'{'");
        result.registerAdvancement();
        this.push();

        NodeCallback callback = () -> {
            Node key = result.register(parseComparisonExpression());
            if (result.getLanguageError() != null) return result;

            Node value = result.register(this.kv());
            if (result.getLanguageError() != null) return result;
            dict.put(key, value);
            return null;
        };

        ParseResult<Node> parseResult;
        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE)) {
            parseResult = callback.call();
            if (parseResult != null) return parseResult;
        }

        while (currentToken.getType().equals(TokenType.COMMA)) {
            this.push();
            result.registerAdvancement();
            parseResult = callback.call();
            if (parseResult != null) return parseResult;
        }
        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return unexpected("'}'");
        result.registerAdvancement();
        this.push();

        return result.success(new MapNode(dict, start, currentToken.getEndPosition().copy()));
    }

    /**
     * Is catcher parse result.
     *
     * @return the parse result
     */
    public ParseResult<Boolean> isCatcher() {
        ParseResult<Boolean> result = new ParseResult<>();
        if (currentToken.getType() == TokenType.LEFT_BRACKET) {
            result.registerAdvancement();
            this.push();

            if (currentToken.getType() != TokenType.RIGHT_BRACKET)
                return unexpected("']'");

            result.registerAdvancement();
            this.push();
            return result.success(true);
        }
        return result.success(false);
    }

    // Executables

    /**
     * Parse inline function definition parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseInlineFunctionDefinition() {
        ParseResult<Node> result = new ParseResult<>();

        String tokV = (String) currentToken.getValue();

        if (!currentToken.getType().equals(TokenType.KEYWORD) && Objects.equals("inline", tokV))
            return unexpected("'function'");

        this.push();
        result.registerAdvancement();

        boolean async = false;
        if (currentToken.matches(TokenType.KEYWORD, "async")) {
            async = true;
            this.push();
            result.registerAdvancement();
        }

        Token varNameTok = null;
        if (currentToken.getType().equals(TokenType.IDENTIFIER)) {
            varNameTok = currentToken;

            result.registerAdvancement();
            this.push();
        }

        Argument argTKs = result.register(parseArguments());
        if (result.getLanguageError() != null) return result;

        boolean isCatcher = result.register(this.isCatcher());
        if (result.getLanguageError() != null) return result;

        List<String> retype = result.register(parseReturnDefinition());
        if (result.getLanguageError() != null) return result;

        Node nodeToReturn;
        switch (currentToken.getType()) {
            case FAT_ARROW -> {
                result.registerAdvancement();
                this.push();
                nodeToReturn = result.register(this.parseStatement());
                if (result.getLanguageError() != null) return result;
                return result.success(new InlineDeclareNode(varNameTok, argTKs.argumentTokenNames,
                        argTKs.argumentTypeTokens, nodeToReturn,
                        true, async, retype, argTKs.defaults, argTKs.defaultCount,
                        argTKs.generics, argTKs.argumentName, argTKs.argumentName).setCatcher(isCatcher));
            }
            case LEFT_BRACE -> {
                nodeToReturn = result.register(this.parseBlock(varNameTok != null));
                if (result.getLanguageError() != null) return result;

                Node funcNode = new InlineDeclareNode(varNameTok, argTKs.argumentTokenNames,
                        argTKs.argumentTypeTokens, nodeToReturn,
                        false, async, retype, argTKs.defaults, argTKs.defaultCount,
                        argTKs.generics, argTKs.argumentName, argTKs.argumentName).setCatcher(isCatcher);

                return result.success(funcNode);
            }
            default -> {
                return unexpected("'->' or '{'");
            }
        }

    }

    /**
     * Parse return definition parse result.
     *
     * @return the parse result
     */
    public ParseResult<List<String>> parseReturnDefinition() {
        ParseResult<List<String>> result = new ParseResult<>();

        List<String> retype = Collections.singletonList("any");

        if (currentToken.getType().equals(TokenType.SKINNY_ARROW) ||
                currentToken.matches(TokenType.KEYWORD, "yields") || currentToken.getType().equals(TokenType.COLON)) {

            result.registerAdvancement();
            this.push();
            Token typeToken = result.register(parseTypeToken());
            if (result.getLanguageError() != null) return result;

            retype = WrappedCast.cast(typeToken.getValue());

        }
        return result.success(retype);
    }

    /**
     * Parse class definition parse result.
     *
     * @return the parse result
     */
    public ParseResult<Node> parseClassDefinition() {
        ParseResult<Node> result = new ParseResult<>();

        if (currentToken.getType() != TokenType.KEYWORD || !currentToken.getValue().toString().equals("class"))
            return unexpected("'class'");
        this.push();
        result.registerAdvancement();
        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
            return unexpected("identifier");


        Token classNameTok = currentToken;
        result.registerAdvancement();
        this.push();

        Token ptk = null;
        if (currentToken.getType() == TokenType.SKINNY_ARROW) {
            this.push();
            result.registerAdvancement();
            if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                return unexpected("identifier");
            ptk = currentToken;
            result.registerAdvancement();
            this.push();
        }


        if (!currentToken.getType().equals(TokenType.LEFT_BRACE))
            return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                    currentToken.getEndPosition().copy(), "Expected '{'"));
        result.registerAdvancement();
        this.push();

        List<AttributeDeclareNode> attributeDeclarations = new ArrayList<>();

        Argument argument = new Argument(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                0, new ArrayList<>(), null, null);

        AttributeGetter getComplexAttribute = (valTok, isStatic, isPrivate) -> {
            ParseResult<Node> attributeResult = new ParseResult<>();

            List<String> type = Collections.singletonList("any");
            if (currentToken.getType() == TokenType.COLON) {
                attributeResult.registerAdvancement();
                this.push();
                Token t = attributeResult.register(parseTypeToken());
                if (attributeResult.getLanguageError() != null) return attributeResult;
                type = WrappedCast.cast(t.getValue());
            }

            Node expr = null;
            if (currentToken.getType().equals(TokenType.FAT_ARROW))
                return attributeResult.failure(LanguageException.expected(currentToken.getStartPosition().copy(),
                        currentToken.getEndPosition().copy(), "Should be '='"));
            if (currentToken.getType() == TokenType.EQUAL) {
                attributeResult.registerAdvancement();
                this.push();
                expr = attributeResult.register(this.parseExpression());
                if (attributeResult.getLanguageError() != null) return attributeResult;
            }

            attributeDeclarations.add(new AttributeDeclareNode(valTok, type, isStatic, isPrivate, expr));

            attributeResult.register(expectSemicolon());
            if (attributeResult.getLanguageError() != null) return attributeResult;
            this.push();
            attributeResult.registerAdvancement();

            return attributeResult;
        };

        Node ingredientNode = new BodyNode(new ArrayList<>(), classNameTok.getStartPosition().copy(), classNameTok.getEndPosition().copy());
        List<MethodDeclareNode> methods = new ArrayList<>();
        while (currentToken.getType().equals(TokenType.KEYWORD) || currentToken.getType().equals(TokenType.IDENTIFIER)) {
            if (currentToken.getValue().toString().equals("constructor")) {
                this.push();
                result.registerAdvancement();
                argument = result.register(parseArguments());
                if (result.getLanguageError() != null) return result;

                ingredientNode = result.register(this.parseBlock(false));
                if (result.getLanguageError() != null) return result;
            } else if ("method".equals(currentToken.getValue().toString())) {
                result.registerAdvancement();
                this.push();

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
                    this.push();
                    result.registerAdvancement();
                }

                if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                    return unexpected("identifier");
                Token methodName = currentToken;

                result.registerAdvancement();
                this.push();
                Argument args = result.register(parseArguments());

                boolean isCatcher = result.register(this.isCatcher());
                if (result.getLanguageError() != null) return result;

                List<String> retype = result.register(parseReturnDefinition());
                if (result.getLanguageError() != null) return result;

                Node nodeToReturn;

                switch (currentToken.getType()) {
                    case FAT_ARROW -> {
                        result.registerAdvancement();
                        this.push();
                        nodeToReturn = result.register(this.parseStatement());
                        if (result.getLanguageError() != null) return result;
                        result.register(expectSemicolon());
                        if (result.getLanguageError() != null) return result;
                        result.registerAdvancement();
                        this.push();
                        methods.add(new MethodDeclareNode(methodName, args.argumentTokenNames,
                                args.argumentTypeTokens, nodeToReturn, true,
                                bin, async, retype, args.defaults, args.defaultCount, args.generics,
                                isStatic, isPrivate, args.argumentName, args.keywordArgument).setCatcher(isCatcher));
                    }
                    case LEFT_BRACE -> {
                        nodeToReturn = result.register(this.parseBlock(false));
                        if (result.getLanguageError() != null) return result;
                        methods.add(new MethodDeclareNode(methodName, args.argumentTokenNames,
                                args.argumentTypeTokens, nodeToReturn, false, bin, async,
                                retype, args.defaults, args.defaultCount, args.generics, isStatic, isPrivate,
                                args.argumentName, args.keywordArgument).setCatcher(isCatcher));
                    }
                    default -> {
                        return unexpected("'{' or '->'");
                    }
                }

            } else if (currentToken.getType().equals(TokenType.IDENTIFIER)) {
                Token valTok = currentToken;


                result.registerAdvancement();
                this.push();
                if (currentToken.getType().equals(TokenType.FAT_ARROW))
                    return result.failure(LanguageException.expected(currentToken.getStartPosition().copy(), currentToken.getEndPosition().copy(), "Should be '='"));
                if (currentToken.getType() == TokenType.EQUAL || currentToken.getType() == TokenType.COLON) {
                    result.register(getComplexAttribute.run(valTok, false, false));
                    if (result.getLanguageError() != null) return result;
                } else {
                    attributeDeclarations.add(new AttributeDeclareNode(valTok));
                    while (currentToken.getType().equals(TokenType.COMMA)) {
                        result.registerAdvancement();
                        this.push();
                        if (!currentToken.getType().equals(TokenType.IDENTIFIER))
                            return unexpected("identifier");


                        attributeDeclarations.add(new AttributeDeclareNode(currentToken));
                        this.push();
                        result.registerAdvancement();
                    }

                    result.register(expectSemicolon());
                    if (result.getLanguageError() != null) return result;
                    this.push();
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
                    this.push();
                }

                if (currentToken.getType() != TokenType.IDENTIFIER)
                    return unexpected("identifier");

                Token valTok = currentToken;


                result.registerAdvancement();
                this.push();
                result.register(getComplexAttribute.run(valTok, isStatic, isPrivate));
                if (result.getLanguageError() != null) return result;
            } else
                return result.failure(LanguageException.invalidSyntax(currentToken.getStartPosition(),
                        currentToken.getEndPosition(), "Unexpected keyword: " + currentToken.asString()));
        }
        if (!currentToken.getType().equals(TokenType.RIGHT_BRACE))
            return unexpected("Expected '}'");

        endLine(1);
        this.push();
        result.registerAdvancement();

        Node classDef = new ClassDefNode(classNameTok, attributeDeclarations, argument.argumentTokenNames, argument.argumentTypeTokens, ingredientNode, methods, currentToken.getEndPosition().copy(), argument.defaults, argument.defaultCount, ptk, argument.generics, argument.argumentName, argument.keywordArgument);

        return result.success(classDef);
    }

    // --- utility methods ---

    /**
     * Push.
     */
    public void push() {
        tokenIndex++;
        updateTokens();
    }

    /**
     * Update tokens.
     */
    public void updateTokens() {
        if (0 <= tokenIndex && tokenIndex < tokenCount) currentToken = tokens.get(tokenIndex);
    }

    /**
     * Peek token.
     *
     * @param i the
     * @return the token
     */
    public Token peek(int i) {
        return 0 <= tokenIndex + i && tokenIndex + i < tokenCount ? tokens.get(tokenIndex + i) : null;
    }

    /**
     * Pull.
     *
     * @param amount the amount
     */
    public void pull(int amount) {
        tokenIndex -= amount;
        updateTokens();
    }

    private String tokenFound() {
        if (currentToken.getValue() != null) return String.valueOf(currentToken.getValue());
        return String.valueOf(currentToken.getType());
    }

    /**
     * Expect boolean.
     *
     * @param expectedSymbol the expected symbol
     * @return the boolean
     */
    public boolean tokenNotMatching(final String expectedSymbol) {
        boolean valid = this.currentToken.getValue() != null && this.currentToken.getValue().equals(expectedSymbol);
        if (valid) {
            this.push();
        }

        return !valid;
    }

    /**
     * Expect boolean.
     *
     * @param expectedSymbol the expected symbol
     * @param present        the present
     * @return the boolean
     */
    public boolean expect(final TokenType expectedSymbol, final Runnable present) {
        boolean valid = this.currentToken.getValue() != null && this.currentToken.getType().equals(expectedSymbol);
        if (valid)
            present.run();

        return valid;
    }

    /**
     * Unexpected parse result.
     *
     * @param <T>            the type parameter
     * @param expectedSymbol the expected symbol
     * @return the parse result
     */
    public <T> ParseResult<T> unexpected(final String expectedSymbol) {
        return new ParseResult<T>().failure(
                LanguageException.expected(currentToken.getStartPosition(),
                        currentToken.getEndPosition(), "Expected %s but got %s".formatted(expectedSymbol, tokenFound())));
    }

    /**
     * Unmatched parse result.
     *
     * @param <T>            the type parameter
     * @param expectedSymbol the expected symbol
     * @return the parse result
     */
    public <T> ParseResult<T> unmatched(final String expectedSymbol) {
        return new ParseResult<T>().failure(
                LanguageException.expected(currentToken.getStartPosition(),
                        currentToken.getEndPosition(), "unmatched %s".formatted(expectedSymbol)));
    }

    /**
     * Match chain boolean.
     *
     * @param chain the chain
     * @return the boolean
     */
    @SuppressWarnings("unused") // this is useful but not a current state
    public boolean matchChain(final TokenType... chain) {
        if (chain.length == 0)
            throw new RuntimeException("can't match empty pattern chain!");

        for (TokenType tokenType : chain) {
            if (this.currentToken.getType().equals(tokenType)) {
                this.push();
            } else
                return false;
        }
        return true;

    }

}
