package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class ExtendNode extends Node {
    public final Token name;

    public ExtendNode(Token name) {
        this.name = name;

        startPosition = name.getStartPosition().copy(); endPosition = name.getEndPosition().copy();
        nodeType = NodeType.IMPORT;
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
        return "extend " + name.getValue();
    }
}
