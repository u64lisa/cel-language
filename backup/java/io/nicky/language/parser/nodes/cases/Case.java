package language.frontend.parser.nodes.cases;

import language.frontend.parser.nodes.Node;

public class Case {
    private final boolean returnValue;
    private final Node condition;
    private final Node statements;

    public Case(Node condition, Node statements, boolean returnValue) {
        this.condition = condition;
        this.statements = statements;
        this.returnValue = returnValue;
    }

    @Override
    public String toString() {
        return "Case{" +
                "returnValue=" + returnValue +
                ", condition=" + condition +
                ", statements=" + statements +
                '}';
    }

    public boolean isReturnValue() {
        return returnValue;
    }

    public Node getCondition() {
        return condition;
    }

    public Node getStatements() {
        return statements;
    }
}
