package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.List;

public class WhileNode extends Node {
    public final Node condition;
    public final Node body;
    public final boolean retnull;
    public final boolean conLast;

    public WhileNode(Node condition, Node body, boolean retnull, boolean conLast) {
        this.condition = condition;
        this.body = body;
        this.retnull = retnull;
        this.conLast = conLast;
        startPosition = condition.getStartPosition().copy(); endPosition = body.getEndPosition().copy();
        nodeType = NodeType.WHILE;
    }

    @Override
    public Node optimize() {
        Node condition = this.condition.optimize();
        Node body = this.body.optimize();
        return new WhileNode(condition, body, retnull, conLast);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(condition);
        children.add(body);
        return children;
    }

    @Override
    public String visualize() {
        return "while";
    }
}
