package language.frontend.parser.nodes.variables;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;


import java.util.ArrayList;
import java.util.List;

public class VarNode extends Node {
    public final Object value;
    public final boolean locked;
    public Integer min = null;
    public Integer max = null;

    public VarNode(Object value, boolean locked) {
        this.value = value;
        this.locked = locked;
        nodeType = NodeType.VAR;
    }

    public VarNode setRange(Integer min, Integer max) {
        this.min = min;
        this.max = max;
        return this;
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
        return "VarNode";
    }
}
