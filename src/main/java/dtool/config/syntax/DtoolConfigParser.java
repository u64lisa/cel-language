package dtool.config.syntax;

import dtool.config.syntax.tree.ConfigTree;
import dtool.config.syntax.utils.ErrorUtil;
import dtool.config.syntax.utils.ISyntaxPos;
import dtool.config.syntax.utils.Position;

import java.util.List;
import java.util.function.Supplier;

public class DtoolConfigParser {

    private final ConfigTree configTree = new ConfigTree();

    private final String fileContent;

    private final DtoolToken[] tokens;
    private int index = 0;

    public DtoolConfigParser(final String fileContent, List<DtoolToken> tokenList) {
        this.fileContent = fileContent;
        this.tokens = tokenList.toArray(new DtoolToken[0]);
    }

    public ConfigTree parse() {
        while (hasMore()) {

            switch (current().tokenType) {
                case PROJECT_PROPERTY_KEY -> {
                    advance();

                    tryMatchOrError(DtoolTokenType.DOT);
                    advance();

                    tryMatchOrError(DtoolTokenType.IDENTIFIER);

                    String key = current().value;
                    advance();

                    tryMatchOrError(DtoolTokenType.EQUALS);
                    advance();

                    tryMatchOrError(DtoolTokenType.STRING);
                    String value = current().value
                            .substring(1, current().value.length() - 1);

                    this.configTree.getProjectProperties().put(key, value);
                }
                case DEVELOPMENT_TAG -> {
                    advance();

                    tryMatchOrError(DtoolTokenType.L_CURLY);
                    advance();

                    while (current().tokenType != DtoolTokenType.R_CURLY) {
                        tryMatchOrError(DtoolTokenType.IDENTIFIER);

                        String owner = current().value
                                .substring(1, current().value.length() - 1);;
                        advance();

                        if (current().tokenType == DtoolTokenType.DOT) {
                            tryMatchOrError(DtoolTokenType.DOT);
                            advance();

                            tryMatchOrError(DtoolTokenType.IDENTIFIER);
                            String key = current().value
                                    .substring(1, current().value.length() - 1);;
                            advance();

                            tryMatchOrError(DtoolTokenType.EQUALS);
                            advance();

                            tryMatchOrError(DtoolTokenType.STRING);
                            String value = current().value
                                    .substring(1, current().value.length() - 1);;
                            advance();

                            this.configTree.getDevelopmentTag().put(owner, key, value);
                            continue;
                        }

                        tryMatchOrError(DtoolTokenType.EQUALS);
                        advance();

                        tryMatchOrError(DtoolTokenType.STRING);
                        String value = current().value;
                        advance();

                        this.configTree.getDevelopmentTag().put(owner, value);
                    }
                }
                case MODULE -> {
                    this.advance();

                    tryMatchOrError(DtoolTokenType.STRING);
                    String path = current().value
                            .substring(1, current().value.length() - 1);
                    advance();

                    tryMatchOrError(DtoolTokenType.NUMBER);
                    String priority = current().value;

                    // todo
                }
                case DEPEND_TAG -> {
                    advance();

                    tryMatchOrError(DtoolTokenType.L_CURLY);
                    advance();

                    while (current().tokenType != DtoolTokenType.R_CURLY) {

                        tryMatchOrError(DtoolTokenType.RESOLVE);
                        advance();

                        tryMatchOrError(DtoolTokenType.STRING);
                        String libraryDetails = current().value
                                .substring(1, current().value.length() - 1);;
                        advance();

                        tryMatchOrError(DtoolTokenType.FROM);
                        advance();

                        tryMatchOrError(DtoolTokenType.STRING);
                        String source = current().value
                                .substring(1, current().value.length() - 1);;

                        advance();

                        this.configTree.getDependTag().put(libraryDetails, source);
                    }
                }
                case PLUGIN_TAG -> {
                    advance();

                    tryMatchOrError(DtoolTokenType.L_CURLY);
                    advance();

                    while (current().tokenType != DtoolTokenType.R_CURLY) {

                        tryMatchOrError(DtoolTokenType.RESOLVE);
                        advance();

                        tryMatchOrError(DtoolTokenType.STRING);
                        String pluginDetails = current().value
                                .substring(1, current().value.length() - 1);;
                        advance();

                        tryMatchOrError(DtoolTokenType.FROM);
                        advance();

                        tryMatchOrError(DtoolTokenType.STRING);
                        String source = current().value
                                .substring(1, current().value.length() - 1);;

                        advance();

                        this.configTree.getPluginsTag().put(pluginDetails, source);
                    }
                }

                default -> throw createParseException(current().syntaxPosition, "Unexpected TokenType %s".formatted(current().tokenType));
            }

            advance();
        }

        return configTree;
    }

    DtoolToken current() {
        return tokens[index];
    }

    void advance() {
        this.index++;
    }

    boolean hasMore() {
        return index < tokens.length;
    }

    ParseException createParseException(String format, Object... args) {
        return createParseException(tokens == null ? null : current().syntaxPosition, format, args);
    }

    ParseException createParseException(ISyntaxPos syntaxPosition, String format, Object... args) {
        return createParseException(syntaxPosition.getPath(), syntaxPosition, format, args);
    }

    ParseException createParseException(String path, ISyntaxPos syntaxPosition, String format, Object... args) {
        String msg = String.format(format, args);

        StringBuilder sb = new StringBuilder();
        if (path == null) {
            sb.append("(?) ");
        } else {
            sb.append("(").append(path).append(") ");
        }

        if (syntaxPosition == null) {
            sb.append("(line: ?, column: ?): ").append(msg);
        } else {
            Position position = syntaxPosition.getStartPosition();
            sb.append("(line: ").append(position.line() + 1).append(", column: ").append(position.column() + 1).append("): ")
                    .append(ErrorUtil.createError(syntaxPosition, fileContent, msg));
        }

        return new ParseException(sb.toString());
    }

    void tryMatchOrError(DtoolTokenType tokenType) throws ParseException {
        tryMatchOrError(tokenType, () -> "Expected %s but got %s".formatted(tokenType, current().tokenType));
    }

    void tryMatchOrError(DtoolTokenType tokenType, Supplier<String> message) throws ParseException {
        if (current().tokenType != tokenType) {
            throw createParseException(current().syntaxPosition, message.get());
        }

    }


}
