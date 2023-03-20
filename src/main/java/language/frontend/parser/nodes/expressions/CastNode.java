package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class CastNode extends Node {
    public final Node expr;
    public final Token type;

    public CastNode(Node expr, Token type) {
        this.expr = expr;
        this.type = type;
        startPosition = type.getStartPosition();
        endPosition = expr.getEndPosition();
        nodeType = NodeType.CAST;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(expr);
        return children;
    }

    @Override
    public String visualize() {
        return "| " + String.join("", (List<String>) type.getValue()) + " |";
    }
}
