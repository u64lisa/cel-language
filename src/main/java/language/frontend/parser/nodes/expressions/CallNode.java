package language.frontend.parser.nodes.expressions;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import language.frontend.parser.nodes.Node;

import java.util.*;

public class CallNode extends Node {
    public final Node nodeToCall;
    public final List<Node> argNodes;
    public final Map<String, Node> kwargs;
    public final List<Token> generics;

    public CallNode(Node nodeToCall, List<Node> argNodes, List<Token> generics, Map<String, Node> kwargs) {
        this.nodeToCall = nodeToCall;
        this.argNodes = argNodes;
        this.generics = generics;
        this.kwargs = kwargs;

        startPosition = nodeToCall.getStartPosition().copy();
        endPosition = (argNodes != null && argNodes.size() > 0 ? argNodes.get(argNodes.size() - 1) : nodeToCall).getEndPosition().copy();
        nodeType = NodeType.CALL;
    }

    @Override
    public Node optimize() {
        List<Node> optimizedArgNodes = new ArrayList<>();
        for (Node argNode : argNodes) {
            Node optimized = argNode.optimize();
            if (optimized.getNodeType() == NodeType.SPREAD && optimized.isConstant()) {
                SpreadNode spread = (SpreadNode) optimized;
                if (spread.internal.getNodeType() == NodeType.LIST) {
                    for (Node node : spread.internal.asList())
                        optimizedArgNodes.add(node.optimize());
                }
            }
            else {
                optimizedArgNodes.add(optimized);
            }
        }

        Map<String, Node> optimizedKwargs = new HashMap<>();
        for (Map.Entry<String, Node> entry : kwargs.entrySet()) {
            Node optimized = entry.getValue().optimize();
            optimizedKwargs.put(entry.getKey(), optimized);
        }

        return new CallNode(nodeToCall, optimizedArgNodes, generics, optimizedKwargs);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(nodeToCall);
        children.addAll(argNodes);
        children.addAll(kwargs.values());
        return children;
    }

    @Override
    public String visualize() {
        return "call";
    }
}
