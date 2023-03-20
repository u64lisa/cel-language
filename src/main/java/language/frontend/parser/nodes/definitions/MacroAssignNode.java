package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MacroAssignNode extends Node {
    public final Token name;
    public final Node value;

    public MacroAssignNode(Token name, Node value) {
        this.name = name;
        this.value = value;

        startPosition = name.getStartPosition(); endPosition = name.getEndPosition();
        nodeType = NodeType.DYNAMIC_ASSIGN;
    }

    @Override
    public Node optimize() {
        return new MacroAssignNode(name, value.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(value));
    }

    @Override
    public String visualize() {
        return "macro";
    }
}
