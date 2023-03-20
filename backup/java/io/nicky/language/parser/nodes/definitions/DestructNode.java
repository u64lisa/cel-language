package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;

import java.util.*;

public class DestructNode extends Node {
    public final Node target;
    public List<Token> subs = new ArrayList<>();
    public boolean glob = false;

    public DestructNode(Node tar) {
        target = tar;
        glob = true;

        startPosition = tar.getStartPosition();
        endPosition = tar.getEndPosition();
        nodeType = NodeType.DESTRUCT;
    }

    public DestructNode(Node tar, List<Token> tars) {
        target = tar;
        subs = tars;

        startPosition = tars.get(0).getStartPosition();
        endPosition = tar.getEndPosition();
        nodeType = NodeType.DESTRUCT;
    }

    @Override
    public Node optimize() {
        Node target = this.target.optimize();
        return new DestructNode(target, subs);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(target));
    }

    @Override
    public String visualize() {
        if (glob) {
            return "destruct *";
        }
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("destruct ");
            for (Token struct : subs)
                sb.append(struct.getValue().toString()).append(" ");
            return sb.substring(0, sb.length() - 1);
        }
    }
}
