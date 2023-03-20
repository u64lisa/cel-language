package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;


import java.util.ArrayList;
import java.util.List;

public class ListNode extends Node {
    public final List<Node> elements;

    public ListNode(List<Node> elements, Position startPosition, Position endPosition) {
        this.elements = elements;

        constant = true;
        for (Node element : elements) {
            if (!element.isConstant()) {
                constant = false;
                break;
            }
        }

        this.startPosition = startPosition.copy();
        this.endPosition = endPosition.copy();
        nodeType = NodeType.LIST;
    }

    @Override
    public Node optimize() {
        List<Node> optimizedElements = new ArrayList<>();
        for (Node element : elements) {
            optimizedElements.add(element.optimize());
        }
        return new ListNode(optimizedElements, getStartPosition(), getEndPosition());
    }

    @Override
    public double asNumber() {
        return elements.size();
    }

    @Override
    public List<Node> asList() {
        return elements;
    }

    @Override
    public String asString() {
        StringBuilder result = new StringBuilder("[");
        elements.forEach(k -> {
            if (k.getNodeType() == NodeType.STRING) {
                result.append('"').append(k.asString()).append('"');
            } else {
                result.append(k.asString());
            }
            result.append(", ");
        });
        if (result.length() > 1) {
            result.setLength(result.length() - 2);
        }
        result.append("]");
        return result.toString();
    }

    @Override
    public boolean asBoolean() {
        return !elements.isEmpty();
    }

    @Override
    public boolean equals(Node other) {
        if (other.getNodeType() != NodeType.LIST)
            return false;
        ListNode otherList = (ListNode) other;
        if (otherList.elements.size() != elements.size())
            return false;
        for (int index = 0; index < elements.size(); index++) {
            if (!elements.get(index).equals(otherList.elements.get(index)))
                return false;
        }
        return true;
    }

    @Override
    public List<Node> getChildren() {
        return elements;
    }

    @Override
    public String visualize() {
        return "[ List ]";
    }
}
