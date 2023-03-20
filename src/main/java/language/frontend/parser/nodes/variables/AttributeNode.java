package language.frontend.parser.nodes.variables;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;


import java.util.ArrayList;
import java.util.List;

public class AttributeNode extends Node {
    public final Object value;

    public AttributeNode(Object value) {
        this.value = value;
        nodeType = NodeType.ATTRIBUTE;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        return "AttrNode";
    }
}

