package language.frontend.parser.nodes.expressions;

import language.frontend.parser.nodes.NodeType;

import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThrowNode extends Node {
    public final Node thrown;
    public final Node throwType;

    public ThrowNode(Node throwType, Node thrown) {
        this.thrown = thrown;
        this.throwType = throwType;
        startPosition = throwType.getStartPosition(); endPosition = thrown.getEndPosition();
        nodeType = NodeType.THROW;
    }

    @Override
    public Node optimize() {
        return new ThrowNode(throwType.optimize(), thrown.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Arrays.asList(throwType, thrown));
    }

    @Override
    public String visualize() {
        return "throw";
    }
}
