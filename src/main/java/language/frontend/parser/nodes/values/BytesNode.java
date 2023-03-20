package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BytesNode extends Node {
    public final Node toBytes;

    public BytesNode(Node toBytes) {
        this.toBytes = toBytes;

        this.startPosition = toBytes.getStartPosition().copy();
        this.endPosition = toBytes.getEndPosition().copy();

        nodeType = NodeType.BYTES;
    }

    @Override
    public Node optimize() {
        return new BytesNode(toBytes.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(toBytes));
    }

    @Override
    public String visualize() {
        return "@";
    }
}
