package language.frontend.parser.nodes.variables;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.Collections;
import java.util.List;

public class TypeDefinitionNode extends Node {

    private final Token type;
    private final Token name;

    public TypeDefinitionNode(Token type, Token name) {
        startPosition = type.getStartPosition();
        endPosition = name.getEndPosition();

        this.type = type;
        this.name = name;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public String visualize() {
        return type.asString() + " renamed to -> " + name.asString();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.TYPE_DEFINITION;
    }

    public Token getType() {
        return type;
    }

    public Token getName() {
        return name;
    }
}
