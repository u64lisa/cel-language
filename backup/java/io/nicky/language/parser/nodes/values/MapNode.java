package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapNode extends Node {
    public final Map<Node, Node> dict;

    public MapNode(Map<Node, Node> dict, Position startPosition, Position endPosition) {
        this.dict = dict;

        constant = true;
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            if (!entry.getKey().isConstant() || !entry.getValue().isConstant()) {
                constant = false;
                break;
            }
        }

        this.startPosition = startPosition.copy();
        this.endPosition = endPosition.copy();
        nodeType = NodeType.MAP;
    }

    @Override
    public Node optimize() {
        Map<Node, Node> newDict = new ConcurrentHashMap<>();
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            Node val = entry.getValue().optimize();
            Node key = entry.getKey().optimize();
            newDict.put(key, val);
        }
        return new MapNode(newDict, getStartPosition(), getEndPosition());
    }

    @Override
    public boolean asBoolean() {
        return !dict.isEmpty();
    }

    @Override
    public String asString() {
        StringBuilder result = new StringBuilder("{");
        dict.forEach((k, v) -> {
            if (k.getNodeType() == NodeType.STRING) {
                result.append('"').append(k.asString()).append('"');
            }
            else {
                result.append(k.asString());
            }
            result.append(": ");
            if (v.getNodeType() == NodeType.STRING) {
                result.append('"').append(v.asString()).append('"');
            }
            else {
                result.append(v.asString());
            }
            result.append(", ");
        });
        if (result.length() > 1) {
            result.setLength(result.length() - 2);
        } result.append("}");
        return result.toString();
    }

    @Override
    public Map<Node, Node> asMap() {
        return dict;
    }

    @Override
    public double asNumber() {
        return dict.size();
    }

    @Override
    public boolean equals(Node other) {
        if (other.getNodeType() != NodeType.MAP) return false;
        MapNode otherDict = (MapNode) other;
        if (dict.size() != otherDict.dict.size()) return false;
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            Node key = entry.getKey();
            Node value = entry.getValue();
            if (!otherDict.dict.containsKey(key)) return false;
            if (!otherDict.dict.get(key).equals(value)) return false;
        } return true;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        for (Map.Entry<Node, Node> entry : dict.entrySet()) {
            children.add(entry.getKey());
            children.add(entry.getValue());
        } return children;
    }

    @Override
    public String visualize() {
        return "{ Map }";
    }
}
