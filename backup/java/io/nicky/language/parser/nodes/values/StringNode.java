package language.frontend.parser.nodes.values;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;

import language.utils.Pair;
import language.utils.StringUnescape;

public class StringNode extends ValueNode {
    public final String val;

    public StringNode(Token tok) {
        super(tok);
        val = ((Pair<String, Boolean>) tok.getValue()).getFirst();
        nodeType = NodeType.STRING;
    }

    public StringNode(String val, Position start, Position end) {
        super(new Token(TokenType.STRING, new Pair<>(val, false), start, end));
        this.val = val;
        nodeType = NodeType.STRING;
    }

    @Override
    public boolean asBoolean() {
        return !val.isEmpty();
    }

    @Override
    public String asString() {
        return val;
    }

    @Override
    public double asNumber() {
        return val.length();
    }

    @Override
    public boolean equals(Node other) {
        if (other instanceof StringNode) {
            return val.equals(((StringNode) other).val);
        }
        return false;
    }

    @Override
    public String visualize() {
        return "\"" + StringUnescape.unescapeJavaString(val) + "\"";
    }
}
