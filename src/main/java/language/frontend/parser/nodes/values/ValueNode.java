package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public class ValueNode extends Node {
    public final Token tok;

    public ValueNode(Token tok) {
        this.tok = tok;
        startPosition = tok.getStartPosition();
        endPosition = tok.getEndPosition();
        nodeType = NodeType.VALUE;
        constant = true;
    }

    public String toString() {
        return tok.toString();
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
        return toString();
    }
}
