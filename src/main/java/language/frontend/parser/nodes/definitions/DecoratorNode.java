package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class DecoratorNode extends Node {
    public Node decorator;
    public Node decorated;
    public Token name;

    public DecoratorNode(Node decorator, Node fn, Token name) {
        this.decorator = decorator;
        this.decorated = fn;
        this.name = name;

        startPosition = decorator.getStartPosition();
        endPosition = decorated.getEndPosition();
        nodeType = NodeType.DECORATOR;
    }

    @Override
    public Node optimize() {
        return new DecoratorNode(decorator.optimize(), decorated.optimize(), name);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(decorator);
        children.add(decorated);
        return children;
    }

    @Override
    public String visualize() {
        return "/decorator de " + name.getValue() + "/";
    }
}
