package language.frontend.parser.nodes.expressions;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public final class PackageNode extends Node {

    public final Token name;
    public final Token as;

    public PackageNode(Token name) {
        this.name = name;
        this.as = null;

        startPosition = name.getStartPosition().copy();
        endPosition = name.getEndPosition().copy();
        nodeType = NodeType.PACKAGE;
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
        return "package " + name.getValue();
    }
}
