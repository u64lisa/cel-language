package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VarAssignNode extends Node {
    public final Token name;
    public final Node value;
    public final boolean locked;
    public boolean defining;
    public Integer min = null;
    public Integer max = null;
    public List<String> type;

    public VarAssignNode setType(List<String> type) {
        this.type = type;
        return this;
    }

    public VarAssignNode setDefining(boolean defining) {
        this.defining = defining;
        return this;
    }

    public VarAssignNode setRange(Integer min, Integer max) {
        this.max = max;
        this.min = min;
        return this;
    }

    public VarAssignNode(Token name, Node value) {
        this.name = name;
        this.value = value;

        locked = false;
        defining = true;
        startPosition = name.getStartPosition(); endPosition = name.getEndPosition();
        nodeType = NodeType.VAR_ASSIGNMENT;
    }

    public VarAssignNode(Token name, Node value, boolean locked) {
        this.name = name;
        this.value = value;
        this.locked = locked;

        defining = true;
        startPosition = name.getStartPosition(); endPosition = name.getEndPosition();
        nodeType = NodeType.VAR_ASSIGNMENT;

    }

    @SuppressWarnings("unused")
    public VarAssignNode(Token name, Node value, boolean defining, int _x) {
        this.name = name;
        this.value = value;
        locked = false;

        this.defining = defining;
        startPosition = name.getStartPosition(); endPosition = name.getEndPosition();
        nodeType = NodeType.VAR_ASSIGNMENT;
    }

    @Override
    public Node optimize() {
        Node val = value.optimize();
        return new VarAssignNode(name, val, locked)
                .setDefining(defining)
                .setRange(min, max)
                .setType(type);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(value));
    }

    @Override
    public String visualize() {
        return "var " + name.getValue();
    }
}
