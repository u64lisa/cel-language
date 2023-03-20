package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;


public class BooleanNode extends ValueNode {
    public final boolean val;

    public BooleanNode(Token tok) {
        super(tok);
        val = (boolean) tok.getValue();
        nodeType = NodeType.BOOLEAN;
    }

    public BooleanNode(boolean val, Position start, Position end) {
        super(new Token(TokenType.BOOLEAN, start, end));
        this.val = val;
        nodeType = NodeType.BOOLEAN;
    }

    @Override
    public double asNumber() {
        return val ? 1 : 0;
    }

    @Override
    public boolean asBoolean() {
        return val;
    }

    @Override
    public String asString() {
        return String.valueOf(val);
    }

    @Override
    public boolean equals(Node other) {
        if (other instanceof BooleanNode) {
            return val == ((BooleanNode) other).val;
        }
        return false;
    }

    @Override
    public String visualize() {
        return String.valueOf(val);
    }
}
