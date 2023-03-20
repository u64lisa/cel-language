package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.*;

public class PatternNode extends Node {
    public final Node accessNode;
    public final HashMap<Token, Node> patterns;

    public PatternNode(Node accessNode, HashMap<Token, Node> patterns) {
        this.accessNode = accessNode;
        this.patterns = patterns;

        startPosition = accessNode.getStartPosition(); endPosition = accessNode.getEndPosition();
        nodeType = NodeType.PATTERN;
    }

    @Override
    public Node optimize() {
        accessNode.optimize();
        for (Node node : patterns.values())
            node.optimize();
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(accessNode));
    }

    @Override
    public String visualize() {
        return "Pattern";
    }
}
