package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReturnNode extends Node {
    public final Node nodeToReturn;

    public ReturnNode(Node nodeToReturn, Position startPosition, Position endPosition) {
        this.nodeToReturn = nodeToReturn;
        this.startPosition = startPosition.copy(); this.endPosition = endPosition.copy();
        nodeType = NodeType.RETURN;
    }

    @Override
    public Node optimize() {
        return new ReturnNode(nodeToReturn != null ? nodeToReturn.optimize() : null, getStartPosition(), getEndPosition());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(nodeToReturn));
    }

    @Override
    public String visualize() {
        return "return";
    }
}
