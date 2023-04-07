package dtool.config.syntax;

import dtool.config.syntax.utils.ErrorUtil;
import dtool.config.syntax.utils.ISyntaxPos;
import dtool.config.syntax.utils.Position;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DtoolConfigLexer {
    public static final GenericLexerContext<DtoolTokenType> LEXER;

    static {
        LEXER = new GenericLexerContext<DtoolTokenType>()
                // Whitespaces
                .addRule(DtoolTokenType.WHITESPACE, i -> i
                        .addMultiline("/*", "*/")
                        .addRegex("//[^\\r\\n]*")
                        .addRegex("[ \t\r\n]+")
                )

                .addRule(DtoolTokenType.L_CURLY, i -> i.addString("{"))
                .addRule(DtoolTokenType.R_CURLY, i -> i.addString("}"))
                .addRule(DtoolTokenType.DOT, i -> i.addString("."))
                .addRule(DtoolTokenType.EQUALS, i -> i.addString("="))

                // Atoms
                .addRule(DtoolTokenType.IDENTIFIER, i -> i.addRegex("[a-zA-Z_][a-zA-Z0-9_]*"))
                .addRule(DtoolTokenType.NUMBER, i-> i.addRegex("[0-9]*"))
                .addRule(DtoolTokenType.STRING, i -> i
                        .addMultiline("'", "\\", "'")
                        .addMultiline("\"", "\\", "\""))

                .addRule(DtoolTokenType.PROJECT_PROPERTY_KEY, i -> i.addString("project"))
                .addRule(DtoolTokenType.DEPEND_TAG, i -> i.addString("depend"))
                .addRule(DtoolTokenType.DEVELOPMENT_TAG, i -> i.addString("development"))
                .addRule(DtoolTokenType.PLUGIN_TAG, i -> i.addString("plugins"))
                .addRule(DtoolTokenType.MODULE, i -> i.addString("module"))

                .addRule(DtoolTokenType.RESOLVE, i -> i.addString("resolve"))
                .addRule(DtoolTokenType.FROM, i -> i.addString("from"))

                .toImmutable();
    }

    public List<DtoolToken> parse(String path, byte[] bytes) {
        List<DtoolToken> tokenList = parseKeepWhitespace(path, bytes);
        tokenList.removeIf(token -> token.tokenType == DtoolTokenType.WHITESPACE);
        return tokenList;
    }

    public List<DtoolToken> parseKeepWhitespace(String path, byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        List<DtoolToken> tokenList = new ArrayList<>();
        int offset = 0;
        int line = 0;
        int column = 0;
        int length = text.length();
        String input = text;

        while (offset < length) {
            Position startPos = new Position(column, line); // offset

            LexerToken<DtoolTokenType> lexerToken = LEXER.nextToken(input);
            if (lexerToken == null) {
                throw new RuntimeException(ErrorUtil.createFullError(
                        ISyntaxPos.of(path, startPos, startPos),
                        text,
                        "Could not parse token"
                ));
            }

            if (lexerToken.length + offset > length) {
                break;
            }

            for (int i = offset; i < offset + lexerToken.length; i++) {
                char c = text.charAt(i);

                if (c == '\n') {
                    line++;
                    column = 0;
                } else {
                    column += (c == '\t') ? 4 : 1;
                }
            }

            Position endPos = new Position(column, line); // offset + lexerToken.length
            tokenList.add(new DtoolToken(
                    lexerToken.type,
                    lexerToken.content,
                    ISyntaxPos.of(path, startPos, endPos)
            ));

            input = input.substring(lexerToken.length);
            offset += lexerToken.length;
        }

        return tokenList;
    }
}

