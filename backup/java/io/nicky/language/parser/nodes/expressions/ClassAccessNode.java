package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassAccessNode extends Node {
    public final Node className;
    public final Token attributeName;

    public ClassAccessNode(Node cls, Token atr) {
        className = cls;
        attributeName = atr;
        startPosition = cls.getStartPosition().copy(); endPosition = atr.getEndPosition().copy();
        nodeType = NodeType.CLASS_ACCESS;
    }

    @Override
    public Node optimize() {
        return new ClassAccessNode(className.optimize(), attributeName);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(className));
    }

    @Override
    public String visualize() {
        return "access::" + attributeName.getValue();
    }
}
