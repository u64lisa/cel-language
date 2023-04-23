package language.frontend.parser.nodes.extra;

import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.List;

public class CompilerNode extends Node {

    private final Token name;
    private final Token type;
    private final Token[] code;

    public CompilerNode(Token name, Token type, Token... code) {
        this.name = name;
        this.type = type;
        this.code = code;

        this.nodeType = NodeType.COMPILER;
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
        return "name: " + name + " compiler: " + type + " code-length: " + code.length;
    }

    public Token getName() {
        return name;
    }

    public Token getType() {
        return type;
    }

    public Token[] getCode() {
        return code;
    }
}
