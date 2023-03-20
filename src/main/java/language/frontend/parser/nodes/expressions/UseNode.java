package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public class UseNode extends Node {
    public final Token useToken;
    public final List<Token> args;

    public UseNode(Token useToken, List<Token> args) {
        this.useToken = useToken;
        this.args = args;
        startPosition = useToken.getStartPosition().copy(); endPosition = useToken.getEndPosition().copy();
        nodeType = NodeType.USE;
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
        String[] args = new String[this.args.size() + 1];
        for (int i = 0; i < args.length - 1; i++) {
            args[i + 1] = this.args.get(i).getValue().toString();
        }
        args[0] = useToken.getValue().toString();
        return "#" + String.join(" ", args);
    }
}
