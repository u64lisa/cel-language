package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

public class NullNode extends ValueNode {
    public NullNode(Token tok) {
        super(tok);
        nodeType = NodeType.NULL;
    }

    @Override
    public boolean equals(Node other) {
        return other instanceof NullNode;
    }

    @Override
    public String visualize() {
        return "null";
    }
}
