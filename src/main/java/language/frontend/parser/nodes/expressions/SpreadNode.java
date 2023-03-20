package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpreadNode extends Node {
    public final Node internal;

    public SpreadNode(Node internal) {
        this.internal = internal;
        startPosition = internal.getStartPosition(); endPosition = internal.getEndPosition();
        this.nodeType = NodeType.SPREAD;
        constant = internal.isConstant();
    }

    @Override
    public Node optimize() {
        return new SpreadNode(internal.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(internal));
    }

    @Override
    public String visualize() {
        return "...";
    }
}
