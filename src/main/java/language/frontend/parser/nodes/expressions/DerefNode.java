package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DerefNode extends Node {
    public final Node ref;

    public DerefNode(Node ref) {
        this.ref = ref;

        startPosition = ref.getStartPosition(); endPosition = ref.getEndPosition();
        nodeType = NodeType.DE_REF;
    }

    @Override
    public Node optimize() {
        return new DerefNode(ref.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(ref));
    }

    @Override
    public String visualize() {
        return "*Ref";
    }
}
