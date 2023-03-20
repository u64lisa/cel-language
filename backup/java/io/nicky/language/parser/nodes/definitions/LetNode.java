package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LetNode extends Node {
    public final Token name;
    public final Node value;

    public LetNode(Token name, Node value) {
        this.name = name;
        this.value = value;

        startPosition = name.getStartPosition(); endPosition = name.getEndPosition();
        nodeType = NodeType.LET;
    }

    @Override
    public Node optimize() {
        Node val = value.optimize();
        return new LetNode(name, val);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(value));
    }

    @Override
    public String visualize() {
        return "let " + name.getValue();
    }
}
