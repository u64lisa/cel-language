package language.backend.compiler.llvm;

import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import dtool.logger.errors.LanguageException;
import language.backend.compiler.AbstractCompiler;
import language.backend.compiler.bytecode.types.Type;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.InlineDeclareNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.nodes.expressions.CallNode;
import language.frontend.parser.nodes.variables.TypeDefinitionNode;

public class LLVMCompiler extends AbstractCompiler {
    public static final Logger SYSTEM_LOGGER = ImplLogger.getInstance();

    private final LLVMTypeLookup typeLookup = new LLVMTypeLookup();

    @Override
    public byte[] compile(String source, List<Node> ast) {

        ast.add(new InlineDeclareNode(
                new Token(TokenType.IDENTIFIER, "println", Position.EMPTY, Position.EMPTY),
                List.of(new Token(TokenType.IDENTIFIER, "none", Position.EMPTY, Position.EMPTY))
                , List.of(new Token(TokenType.IDENTIFIER, "any", Position.EMPTY, Position.EMPTY)),
                new BodyNode(new ArrayList<>(), Position.EMPTY, Position.EMPTY),
                false,
                false, List.of("void"), new ArrayList<>(), 0, new ArrayList<>(),
                null, null
        ));

        CompilerContext context = new CompilerContext(UUID.randomUUID().toString());

        for (TypeDefinitionNode node : ast
                .stream()
                .filter(node -> node.getNodeType().equals(NodeType.TYPE_DEFINITION))
                .map(node -> (TypeDefinitionNode) node)
                .toList()) {
            String typeName = node.getType().asString();
            String name = node.getName().asString();

            Type type = typeLookup.types.getOrDefault(typeName, null);
            if (type == null) {
                this.error("Typedef", "invalid type for '" + typeName + "'");
                return null;
            }

            typeLookup.types.put(name, type);
        }

        ast.removeIf(node -> node.getNodeType() == NodeType.TYPE_DEFINITION);
        ast.stream().map(Node::optimize).forEach(node -> processSingletonNode(context, node));


        context.print();

        return new byte[0];
    }

    private void processSingletonNode(CompilerContext context, Node node) {
        switch(node.getNodeType()) {
            case CLASS_DEFINITION -> {
                processClassDefinition(context, (ClassDefNode) node.optimize());
            }
            case INLINE_DEFINITION -> {
                InlineDeclareNode inlineDeclareNode  = (InlineDeclareNode) node;
                System.out.println(inlineDeclareNode.name);
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


    void error(String type, String message) {
        LanguageException languageException = new LanguageException(
                LanguageException.Type.COMPILER,
                type + " Error", message
        );

        SYSTEM_LOGGER.fail("", "", languageException);
    }

}

