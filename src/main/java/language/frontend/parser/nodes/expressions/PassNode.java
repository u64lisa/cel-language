package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;


import java.util.ArrayList;
import java.util.List;

public class PassNode extends Node {
    public PassNode(Position start, Position end) {
        this.startPosition = start.copy(); this.endPosition = end.copy();
        nodeType = NodeType.PASS;
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
        return "pass";
    }
}
