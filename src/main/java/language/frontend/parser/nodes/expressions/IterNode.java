package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public class IterNode extends Node {
    public final Token name;
    public final Node iterable;
    public final Node body;
    public final boolean retnull;

    public IterNode(Token name, Node iterable, Node body, boolean retnull) {
        this.name = name;
        this.iterable = iterable;
        this.body = body;
        this.retnull = retnull;

        setStartPosition(name.getStartPosition().copy());
        setEndPosition(body.getEndPosition().copy());

        nodeType = NodeType.ITERATOR;
    }

    @Override
    public Node optimize() {
        Node iterable = this.iterable.optimize();
        Node body = this.body.optimize();
        return new IterNode(name, iterable, body, retnull);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(iterable);
        children.add(body);
        return children;
    }

    @Override
    public String visualize() {
        return "iter(" + name.getValue() + ")";
    }
}
