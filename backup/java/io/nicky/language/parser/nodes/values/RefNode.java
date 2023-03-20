package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefNode extends Node {
    public final Node inner;

    public RefNode(Node inner) {
        this.inner = inner;
        startPosition = inner.getStartPosition();
        endPosition = inner.getEndPosition();
        nodeType = NodeType.REFERENCE;
    }

    @Override
    public Node optimize() {
        return new RefNode(inner.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(inner));
    }

    @Override
    public String visualize() {
        return "&Ref";
    }
}
