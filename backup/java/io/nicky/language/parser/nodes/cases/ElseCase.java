package language.frontend.parser.nodes.cases;

import language.frontend.parser.nodes.Node;

public class ElseCase {
    private final boolean returnValue;
    private final Node statements;

    public ElseCase(Node statements, boolean returnValue) {
        this.statements = statements;
        this.returnValue = returnValue;
    }

    public Node getStatements() {
        return statements;
    }

    public boolean isReturnValue() {
        return returnValue;
    }
}
