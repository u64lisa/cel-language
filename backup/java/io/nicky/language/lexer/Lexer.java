package language.frontend.lexer;

import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.utils.Pair;
import language.utils.StringUnescape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Lexer {

    public final String[] keywords = {
            "yields", "destructor", "typedef", "assert", "let",
            "package", "throw", "structure", "do", "loop",
            "pass", "cal", "async", "import", "scope", "as",
            "extend", "bin", "function", "attribute", "constant", "var",
            "for", "while", "null", "if", "elseIf", "else", "return",
            "continue", "break", "method", "ingredients", "recipe", "class",
            "object", "switch", "case", "function", "enum", "default",
            "match", "public", "private", "static", "placeholder",
    };
    public final List<String> keywordList = Arrays.asList(keywords);

    public final String hexDigits = "0123456789ABCDEF";

    public LexicalContext<TokenType> lexicalAnalysesContext;

    private final Position basePosition;

    private final String content;


    public Lexer(final String file, final String text) {
        this.content = text;

        this.basePosition = new Position(-1, 0, -1, file, text);

        lexicalAnalysesContext =
                new LexicalContext<TokenType>()
                        .addRule(TokenType.IGNORED, rule -> rule
                                .addRegex("//[^\\r\\n]*") // single line comment
                                .addMultiline("/*", "*/") // multiline comment
                                .addRegex("[ \t\r\n]+") // ignored
                        )
                        .addRule(TokenType.NEW_LINE, rule -> rule.addString(";"))

                        // combined chars
                        .addRule(TokenType.LEFT_TILDE_ARROW, rule -> rule.addString("<~"))
                        .addRule(TokenType.TILDE_TILDE, rule -> rule.addString("~~"))
                        .addRule(TokenType.RIGHT_TILDE_ARROW, rule -> rule.addString("~>"))
                        .addRule(TokenType.TILDE_AMPERSAND, rule -> rule.addString("~&"))
                        .addRule(TokenType.TILDE_PIPE, rule -> rule.addString("~|"))
                        .addRule(TokenType.TILDE_CARET, rule -> rule.addString("~^"))
                        .addRule(TokenType.FAT_ARROW, rule -> rule.addString("=>"))
                        .addRule(TokenType.EQUAL_EQUAL, rule -> rule.addString("=="))
                        .addRule(TokenType.BANG_EQUAL, rule -> rule.addString("!="))
                        .addRule(TokenType.LEFT_ANGLE, rule -> rule.addString("<"))
                        .addRule(TokenType.RIGHT_ANGLE, rule -> rule.addString(">"))
                        .addRule(TokenType.LESS_EQUALS, rule -> rule.addString("<="))
                        .addRule(TokenType.GREATER_EQUALS, rule -> rule.addString(">="))
                        .addRule(TokenType.COLON_COLON, rule -> rule.addString("::"))
                        .addRule(TokenType.DOLLAR_UNDER_SCORE, rule -> rule.addString("$_"))
                        .addRule(TokenType.SKINNY_ARROW, rule -> rule.addString("->"))
                        .addRule(TokenType.ANGLE_ANGLE, rule -> rule.addString(">>"))
                        .addRule(TokenType.PLUS_EQUALS, rule -> rule.addString("+="))
                        .addRule(TokenType.MINUS_EQUALS, rule -> rule.addString("-="))
                        .addRule(TokenType.START_EQUALS, rule -> rule.addString("*="))
                        .addRule(TokenType.SLASH_EQUALS, rule -> rule.addString("/="))
                        .addRule(TokenType.CARET_EQUALS, rule -> rule.addString("^="))
                        .addRule(TokenType.PLUS_PLUS, rule -> rule.addString("++"))
                        .addRule(TokenType.MINUS_MINUS, rule -> rule.addString("--"))
                        .addRule(TokenType.LEFT_ARROW, rule -> rule.addString("<-"))
                        .addRule(TokenType.DOT_DOT, rule -> rule.addString(".."))

                        // single char
                        .addRule(TokenType.PIPE, rule -> rule.addString("|"))
                        .addRule(TokenType.BANG, rule -> rule.addString("!"))
                        .addRule(TokenType.AMPERSAND, rule -> rule.addString("&"))
                        .addRule(TokenType.DOLLAR, rule -> rule.addString("$"))
                        .addRule(TokenType.BACK_SLASH, rule -> rule.addString("\\"))
                        .addRule(TokenType.DOT, rule -> rule.addString("."))
                        .addRule(TokenType.HASHTAG, rule -> rule.addString("#"))
                        .addRule(TokenType.COMMA, rule -> rule.addString(","))
                        .addRule(TokenType.LEFT_BRACKET, rule -> rule.addString("["))
                        .addRule(TokenType.RIGHT_BRACKET, rule -> rule.addString("]"))
                        .addRule(TokenType.LEFT_BRACE, rule -> rule.addString("{"))
                        .addRule(TokenType.RIGHT_BRACE, rule -> rule.addString("}"))
                        .addRule(TokenType.PERCENT, rule -> rule.addString("%"))
                        .addRule(TokenType.QUESTION_MARK, rule -> rule.addString("?"))
                        .addRule(TokenType.COLON, rule -> rule.addString(":"))
                        .addRule(TokenType.EQUAL, rule -> rule.addString("="))
                        .addRule(TokenType.TILDE, rule -> rule.addString("~"))
                        .addRule(TokenType.AT, rule -> rule.addString("@"))
                        .addRule(TokenType.PLUS, rule -> rule.addString("+"))
                        .addRule(TokenType.MINUS, rule -> rule.addString("-"))
                        .addRule(TokenType.STAR, rule -> rule.addString("*"))
                        .addRule(TokenType.SLASH, rule -> rule.addString("/"))
                        .addRule(TokenType.LEFT_PAREN, rule -> rule.addString("("))
                        .addRule(TokenType.RIGHT_PAREN, rule -> rule.addString(")"))
                        .addRule(TokenType.CARET, rule -> rule.addString("^"))

                        // other
                        .addRule(TokenType.IDENTIFIER, rule -> rule.addRegex("[a-zA-Z_][a-zA-Z0-9_]*"))
                        .addRule(TokenType.BOOLEAN, rule -> rule.addStrings("true", "false"))

                        // numbers
                        .addRule(TokenType.DOUBLE, i -> i.addRegex("[0-9]+(\\.[0-9]+)?[dD]?"))
                        .addRule(TokenType.FLOAT, i -> i.addRegex("[0-9]+(\\.[0-9]+)?[fF]"))
                        .addRule(TokenType.LONG, i -> i.addRegexes(
                                "0b[0-1]+[Ll]",
                                "[0-9]+[Ll]"
                        ))
                        .addRule(TokenType.INTEGER, i -> i.addRegexes(
                                "0b[0-1]+",
                                "[0-9]+"
                        ))
                        .addRule(TokenType.BYTE, i -> i.addRegexes(
                                "0b[0-1]+[Uu]",
                                "[0-9]+[Uu]"
                        ))
                        .addRule(TokenType.SHORT, i -> i.addRegexes(
                                "0b[0-1]+[Uu][Ll]",
                                "[0-9]+[Uu][Ll]"
                        ))

                        .addRule(TokenType.STRING, i -> i
                                .addMultiline("\"", "\\", "\"")
                                .addMultiline("`", "\\", "`")
                                .addMultiline("'", "\\", "'"))

                        .toImmutable();
    }

    public List<Token> lex() {
        List<Token> tokenList = new ArrayList<>();
        int offset = 0;
        int line = 0;
        int column = 0;
        int length = content.length();

        String input = content;

        while (offset < length) {
            Position startPos = basePosition.copy().setSourcePosition(column, line); // offset

            LexicalToken<TokenType> lexerToken = lexicalAnalysesContext.nextToken(input);
            if (lexerToken == null) {
                throw new RuntimeException("Could not parse token at position: " + startPos);
            }

            if (lexerToken.getLength() + offset > length) {
                break;
            }

            for (int i = offset; i < offset + lexerToken.getLength(); i++) {
                char c = content.charAt(i);

                if (c == '\n') {
                    line++;
                    column = 0;
                } else {
                    column += (c == '\t') ? 4 : 1;
                }
            }

            Position endPos = startPos.copy().setCurrent(column, line); // offset + lexerToken.length

            switch (lexerToken.getType()) {

                case IDENTIFIER, KEYWORD -> {
                    tokenList.add(new Token(keywordList.contains(lexerToken.getContent()) ? TokenType.KEYWORD : lexerToken.getType(),
                            lexerToken.getContent(), startPos, endPos));
                }
                case BOOLEAN -> {
                    tokenList.add(new Token(lexerToken.getType(),
                            lexerToken.getContent().equals("true"), startPos, endPos));
                }
                case BYTE, LONG, DOUBLE, FLOAT, INTEGER, SHORT -> {
                    tokenList.add(new Token(lexerToken.getType(),
                            Double.valueOf(lexerToken.getContent()), startPos, endPos));
                }
                case STRING -> {
                    tokenList.add(makeString(lexerToken.getContent(), startPos, endPos));
                }
                default -> {
                    tokenList.add(new Token(lexerToken.getType(),lexerToken.getContent(),
                            startPos, endPos));
                }

            }


            input = input.substring(lexerToken.getLength());
            offset += lexerToken.getLength();
        }
        tokenList.add(new Token(TokenType.EOF, basePosition));

        tokenList.removeIf(token -> token.getType().equals(TokenType.IGNORED));

        return tokenList;
    }

    public int getHexDecimalNumber(String hex) {
        hex = hex.toUpperCase(Locale.ROOT);

        int value = 0;
        for (int index = 0; index < hex.length(); index++) {
            char c = hex.charAt(index);
            int digit = hexDigits.indexOf(c);

            value = 16 * value + digit;
        }
        return value;
    }

    public Token makeString(String current, final Position start, final Position end) {
        StringBuilder string = new StringBuilder();
        boolean escaped = false;

        for (char currentChar : current.substring(1, current.length() - 1).toCharArray()) {
            if (escaped) {
                string.append(StringUnescape.unescapeJavaString("\\" + currentChar));
                escaped = false;
            } else if (currentChar == '\\') {
                escaped = true;
            } else {
                string.append(currentChar);
            }
        }

        return new Token(TokenType.STRING,
                new Pair<>(string.toString(), true), start, end);
    }


}
