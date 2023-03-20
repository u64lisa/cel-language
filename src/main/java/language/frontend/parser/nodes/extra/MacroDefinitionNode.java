package language.frontend.parser.nodes.extra;

import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.List;

public class MacroDefinitionNode extends Node {

    private final String name;
    private final List<Token> body;

    public MacroDefinitionNode(String name, List<Token> body) {
        super();

        this.name = name;
        this.body = body;

        this.nodeType = NodeType.MACRO_DEFINITION;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        StringBuilder tokens = new StringBuilder();
        for (Token token : body) {
            tokens.append(token);
        }

        return name + "-" + tokens.toString();
    }

    public String getName() {
        return name;
    }

    public List<Token> getBody() {
        return body;
    }
}
