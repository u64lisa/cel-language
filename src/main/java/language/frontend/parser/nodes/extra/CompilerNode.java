package language.frontend.parser.nodes.extra;

import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.List;

public class CompilerNode extends Node {

    private final Token type;
    private final Token[] code;

    public CompilerNode(Token type, Token... code) {
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
        return "compiler: " + type + " code-length: " + code.length;
    }

    public Token getType() {
        return type;
    }

    public Token[] getCode() {
        return code;
    }
}
