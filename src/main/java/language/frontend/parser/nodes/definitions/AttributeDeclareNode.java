package language.frontend.parser.nodes.definitions;

import language.frontend.parser.nodes.Node;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttributeDeclareNode extends Node {
    public final Token attrToken;
    public final List<String> type;
    public final boolean isstatic;
    public final boolean isprivate;
    public final Node nValue;
    public final String name;

    public AttributeDeclareNode(Token attrToken) {
        this.attrToken = attrToken;

        type = Collections.singletonList("any");
        isstatic = false;
        isprivate = false;
        nValue = null;

        name = attrToken.getValue().toString();

        startPosition = attrToken.getStartPosition(); endPosition = attrToken.getEndPosition();
    }

    public AttributeDeclareNode(Token attrToken, List<String> type, boolean isstatic, boolean isprivate, Node value) {
        this.attrToken = attrToken;
        this.type = type;
        this.isstatic = isstatic;
        this.isprivate = isprivate;
        this.nValue = value;

        name = attrToken.getValue().toString();

        startPosition = attrToken.getStartPosition(); endPosition = attrToken.getEndPosition();
    }

    @Override
    public Node optimize() {
        return new AttributeDeclareNode(
                attrToken,
                type,
                isstatic,
                isprivate,
                nValue == null ? null : nValue.optimize()
        );
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(nValue));
    }

    @Override
    public String visualize() {
        return "!attr " + name;
    }
}
