package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssertNode extends Node {
    public final Node condition;

    public AssertNode(Node condition) {
        this.condition = condition;
        startPosition = condition.getStartPosition();
        endPosition = condition.getEndPosition();
        nodeType = NodeType.ASSERT;
    }

    @Override
    public Node optimize() {
        return new AssertNode(condition.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(condition));
    }

    @Override
    public String visualize() {
        return "assert";
    }
}
