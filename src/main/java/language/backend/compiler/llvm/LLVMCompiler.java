package language.backend.compiler.llvm;

import language.backend.compiler.AbstractCompiler;
import language.frontend.parser.nodes.Node;

import java.util.List;
import java.util.UUID;

import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import language.frontend.parser.nodes.expressions.CallNode;

public class LLVMCompiler extends AbstractCompiler {
    @Override
    public byte[] compile(String source, List<Node> ast) {
        CompilerContext context = new CompilerContext(UUID.randomUUID().toString());
        ast.stream().map(Node::optimize).forEach(node -> processSingletonNode(context, node));

        context.print();

        return new byte[0];
    }

    private void processSingletonNode(CompilerContext context, Node node) {
        switch(node.getNodeType()) {
            case CLASS_DEFINITION -> {
                processClassDefinition(context, (ClassDefNode) node.optimize());
            }
        }
    }

    private void processClassDefinition(CompilerContext context, ClassDefNode classDefNode) {
        for(Node child: classDefNode.getChildren()) {
            switch (child.getNodeType()) {
                case METHOD_DEFINITION -> {
                    processMethodDeclareNode(context, (MethodDeclareNode) child.optimize());
                }
            }
        }
    }

    private void processMethodDeclareNode(CompilerContext context, MethodDeclareNode methodDeclareNode) {
        List<Node> children = methodDeclareNode.getChildren().get(0).getChildren();
        for(Node child: children) {
            switch(child.getNodeType()) {
                case CALL -> {
                    processCallNode(context, (CallNode) child.optimize());
                }
            }
        }
    }

    private void processCallNode(CompilerContext context, CallNode callNode) {
        System.out.println(callNode.nodeToCall.getNodeType());
    }
}

