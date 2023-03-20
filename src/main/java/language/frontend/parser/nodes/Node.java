package language.frontend.parser.nodes;


import language.frontend.lexer.token.Position;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Node {
    protected Position startPosition;
    protected Position endPosition;
    protected NodeType nodeType;
    protected boolean constant = false;

    public abstract Node optimize();

    public double asNumber() {
        return 0;
    }

    public boolean asBoolean() {
        return false;
    }

    public String asString() {
        return "";
    }

    public Map<Node, Node> asMap() {
        return new ConcurrentHashMap<>();
    }

    public List<Node> asList() {
        return Collections.singletonList(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Node && equals((Node) other);
    }

    public boolean equals(Node other) {
        return false;
    }

    public abstract List<Node> getChildren();

    public abstract String visualize();

    @Override
    public String toString() {
        return "Node{" +
                "startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", nodeType=" + nodeType +
                ", constant=" + constant +
                '}';
    }

    public Node[] asObjectList() {
        return getChildren().toArray(new Node[0]);
    }

    public String asPrintString() {
        return this.visualize();
    }

    protected Node setStatic(boolean b) {
        constant = b;
        return this;
    }

    public Position getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Position startPosition) {
        this.startPosition = startPosition;
    }

    public Position getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Position endPosition) {
        this.endPosition = endPosition;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public boolean isConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }
}
