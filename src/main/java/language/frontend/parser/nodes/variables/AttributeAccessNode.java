package language.frontend.parser.nodes.variables;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public class AttributeAccessNode extends Node {
    public final Token name;

    public AttributeAccessNode(Token name) {
        this.name = name;
        startPosition = name.getStartPosition().copy(); endPosition = name.getEndPosition().copy();
        nodeType = NodeType.ATTRIBUTE_ACCESS;
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
        return name.getValue().toString();
    }
}
