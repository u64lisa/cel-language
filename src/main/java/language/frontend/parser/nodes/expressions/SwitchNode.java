package language.frontend.parser.nodes.expressions;


import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.cases.ElseCase;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;

import java.util.ArrayList;
import java.util.List;

public class SwitchNode extends Node {
    public final boolean match;
    public final List<Case> cases;
    public final ElseCase elseCase;
    public final Node reference;

    public SwitchNode(Node ref, List<Case> cases, ElseCase elseCase, boolean isMatch) {
        this.elseCase = elseCase;
        this.match = isMatch;
        this.cases = cases;
        this.reference = ref;

        startPosition = (cases.size() > 0 ? cases.get(0).getCondition() : ref).getStartPosition().copy();
        endPosition = (elseCase != null ? elseCase.getStatements() :
                (cases.size() > 0 ? cases.get(cases.size() - 1).getCondition() : ref)
        ).getEndPosition().copy();
        nodeType = NodeType.SWITCH;
    }

    @Override
    public Node optimize() {
        Node ref = reference.optimize();
        List<Case> newCases = new ArrayList<>();
        for (Case cs : cases) {
            newCases.add(new Case(cs.getCondition().optimize(), cs.getStatements().optimize(), cs.isReturnValue()));
        }
        ElseCase newElseCase = elseCase != null ? new ElseCase(elseCase.getStatements().optimize(), elseCase.isReturnValue()) : null;
        return new SwitchNode(ref, newCases, newElseCase, match);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(reference);
        for (Case cs : cases) {
            children.add(cs.getCondition());
            children.add(cs.getStatements());
        }
        if (elseCase != null) {
            children.add(elseCase.getStatements());
        }
        return children;
    }

    @Override
    public String visualize() {
        return match ? "match" : "switch";
    }
}
