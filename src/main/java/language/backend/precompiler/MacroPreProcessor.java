package language.backend.precompiler;

import language.frontend.parser.nodes.Node;

import java.util.List;

public class MacroPreProcessor implements PreProcessor {

    @Override
    public List<Node> transform(List<Node> ast) {

        for (Node node : ast) {
            //System.out.println(node.getNodeType());
        }

        return ast;
    }

}
