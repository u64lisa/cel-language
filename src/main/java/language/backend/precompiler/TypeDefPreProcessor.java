package language.backend.precompiler;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.variables.TypeDefinitionNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeDefPreProcessor implements PreProcessor {

    @Override
    public List<Node> transform(List<Node> ast) {
        Map<String, String> translation = new HashMap<>();
        ast.stream()
                .filter(node -> node.getNodeType() == NodeType.TYPE_DEFINITION)
                .map(value -> (TypeDefinitionNode) value)
                .forEach(typeDefinitionNode ->
                        translation.put(typeDefinitionNode.getType().asString(), typeDefinitionNode.getName().asString()));

        ast.removeIf(node -> node.getNodeType().equals(NodeType.TYPE_DEFINITION));
        return ast;

    }

}
