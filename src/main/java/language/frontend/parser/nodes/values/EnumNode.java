package language.frontend.parser.nodes.values;


import language.frontend.parser.units.EnumChild;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.List;

public class EnumNode extends ValueNode {
    public final List<EnumChild> children;
    public final boolean pub;

    public EnumNode(Token tok, List<EnumChild> children, boolean pub) {
        super(tok);
        this.children = children;
        this.pub = pub;
        nodeType = NodeType.ENUM;
    }

    @Override
    public String visualize() {
        return "Enum";
    }
}
