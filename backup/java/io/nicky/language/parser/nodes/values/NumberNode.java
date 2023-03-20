package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;



public class NumberNode extends ValueNode {
    public final double val;
    public final boolean hex;

    public NumberNode(Token tok) {
        super(tok);
        val = (double) tok.getValue();
        hex = false;
        nodeType = NodeType.NUMBER;
    }

    public NumberNode(Token tok, boolean hex) {
        super(tok);
        val = (double) tok.getValue();
        this.hex = hex;
        nodeType = NodeType.NUMBER;
    }

    public NumberNode(int v, Position startPosition, Position endPosition) {
        super(new Token(TokenType.IDENTIFIER, "null", startPosition, endPosition));
        val = v;
        hex = true;
        nodeType = NodeType.NUMBER;
    }

    public NumberNode(double v, Position startPosition, Position endPosition) {
        super(new Token(TokenType.IDENTIFIER, "null", startPosition, endPosition));
        val = v;
        hex = true;
        nodeType = NodeType.NUMBER;
    }

    @Override
    public double asNumber() {
        return val;
    }

    @Override
    public boolean asBoolean() {
        return val != 0;
    }

    @Override
    public String asString() {
        return String.valueOf(val);
    }

    @Override
    public boolean equals(Node other) {
        if (other instanceof NumberNode) {
            return val == ((NumberNode) other).val;
        }
        return false;
    }

    @Override
    public String visualize() {
        return String.valueOf(val);
    }
}
