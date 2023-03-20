package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;
import language.frontend.parser.nodes.Node;


import java.util.List;

public class BodyNode extends Node {
    final public List<Node> statements;

    public BodyNode(List<Node> statements) {
        this.statements = statements;
        this.startPosition = statements.get(0).getStartPosition();
        this.endPosition = statements.get(statements.size() - 1).getEndPosition();
        this.nodeType = NodeType.BODY;
    }

    public BodyNode(List<Node> statements, Position start, Position end) {
        this.statements = statements;
        this.startPosition = start;
        this.endPosition = end;
        this.nodeType = NodeType.BODY;
    }

    @Override
    public Node optimize() {
        for (int i = 0; i < statements.size(); i++) {
            statements.set(i, statements.get(i).optimize());
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return statements;
    }

    @Override
    public String visualize() {
        return "< body >";
    }
}
