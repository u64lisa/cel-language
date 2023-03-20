package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScopeNode extends Node {
    public final Node statements;
    public final String scopeName;

    public ScopeNode(String name, Node states) {
        statements = states;
        scopeName = name;

        startPosition = states.getStartPosition();
        endPosition = states.getEndPosition();
        nodeType = NodeType.SCOPE;
    }

    @Override
    public Node optimize() {
        return new ScopeNode(scopeName, statements.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(statements));
    }

    @Override
    public String visualize() {
        return "scope" + (scopeName == null ? "" : "[" + scopeName + "]");
    }
}
