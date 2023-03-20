package language.frontend.parser.units;

import language.frontend.parser.nodes.Node;
import language.frontend.lexer.token.Token;

import java.util.List;

public class Argument {
    public List<Token> argumentTokenNames;
    public List<Token> argumentTypeTokens;

    public List<Node> defaults;
    public int defaultCount;

    public List<Token> generics;

    public String argumentName;
    public String keywordArgument;

    public Argument(List<Token> argumentTokenNames, List<Token> argumentTypeTokens, List<Node> defaults,
                    int defaultCount, List<Token> generics, String argumentName, String keywordArgument) {

        this.argumentTokenNames = argumentTokenNames;
        this.argumentTypeTokens = argumentTypeTokens;
        this.defaults = defaults;
        this.defaultCount = defaultCount;
        this.generics = generics;
        this.argumentName = argumentName;
        this.keywordArgument = keywordArgument;
    }

}