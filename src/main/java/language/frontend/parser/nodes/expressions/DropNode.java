package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public class DropNode extends Node {
    public final Token varTok;

    public DropNode(Token varTok) {
        startPosition = varTok.getStartPosition(); endPosition = varTok.getEndPosition();
        this.varTok = varTok;
        nodeType = NodeType.DROP;
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
        return "destructor " + varTok.getValue();
    }
}
