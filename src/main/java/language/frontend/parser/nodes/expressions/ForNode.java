package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.Arrays;
import java.util.List;

public class ForNode extends Node {
    public final Token name;
    public final Node start;
    public final Node end;
    public final Node step;
    public final Node body;
    public final boolean retnull;

    public ForNode(Token name, Node start, Node end, Node step, Node body,
                   boolean retnull) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.step = step;
        this.body = body;
        this.retnull = retnull;

        startPosition = name.getStartPosition().copy(); endPosition = body.getEndPosition().copy();
        nodeType = NodeType.FOR;
    }

    @Override
    public Node optimize() {
        Node start = this.start.optimize();
        Node end = this.end.optimize();
        Node step = null;
        if (step != null)
            step = this.step.optimize();
        Node body = this.body.optimize();
        return new ForNode(name, start, end, step, body, retnull);
    }

    @Override
    public List<Node> getChildren() {
        return Arrays.asList(start, end, step, body);
    }

    @Override
    public String visualize() {
        return "for(" + name.getValue() + ")";
    }
}
