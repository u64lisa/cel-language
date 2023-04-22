package language.backend.precompiler;

import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class TestPreProcessor implements PreProcessor {
    @Override
    public List<Node> transform(List<Node> ast) {
        final List<Node> modified = new ArrayList<>();

        modified.addAll(ast);

        return ast;
    }
}
