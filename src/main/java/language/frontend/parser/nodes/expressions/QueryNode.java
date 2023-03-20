package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.cases.ElseCase;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.List;

public class QueryNode extends Node {
    public final List<Case> cases;
    public final ElseCase elseCase;

    public QueryNode(List<Case> cases, ElseCase elseCase) {
        this.elseCase = elseCase;
        this.cases = cases;
        startPosition = cases.get(0).getCondition().getStartPosition().copy(); endPosition = (
                elseCase != null ? elseCase.getStatements() : cases.get(cases.size() - 1).getCondition()
        ).getEndPosition().copy();
        nodeType = NodeType.QUERY;
    }

    @Override
    public Node optimize() {
        List<Case> optimizedCases = new ArrayList<>();
        for (Case aCase : cases) {
            optimizedCases.add(new Case(aCase.getCondition().optimize(), aCase.getStatements().optimize(), aCase.isReturnValue()));
        }
        return new QueryNode(optimizedCases, elseCase != null ? new ElseCase(elseCase.getStatements().optimize(), elseCase.isReturnValue()) : null);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        for (Case aCase : cases) {
            children.add(aCase.getCondition());
            children.add(aCase.getStatements());
        }
        if (elseCase != null) {
            children.add(elseCase.getStatements());
        }
        return children;
    }

    @Override
    public String visualize() {
        return "query";
    }
}
