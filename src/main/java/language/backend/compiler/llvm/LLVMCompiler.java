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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.InlineDeclareNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.nodes.expressions.UseNode;
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

        CompilerContext context = new CompilerContext(typeLookup, UUID.randomUUID().toString());

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

        ast.removeIf(node -> node.getNodeType() == NodeType.TYPE_DEFINITION || node.getNodeType() == NodeType.PACKAGE);
        UseNode mainUseNode = ast.stream()
                .filter(node -> node.getNodeType() == NodeType.USE)
                .map(node -> (UseNode) node)
                .filter(useNode -> Objects.equals(useNode.useToken.getValue().toString(), "main"))
                .findFirst()
                .orElse(null);

        assert mainUseNode != null : "main class not found!";
        String mainClass = mainUseNode.args.get(0).asString();

        context.setMainClassName(mainClass);

        ast.stream()
                .map(Node::optimize)
                .forEach(node -> processSingletonNode(context, null, node));

        context.print();

        String content = "Test";

        return content.getBytes(StandardCharsets.UTF_8);
    }

    private void processSingletonNode(CompilerContext context, Node parent, Node node) {
        switch(node.getNodeType()) {
            case CLASS_DEFINITION -> {
                ClassDefNode classDefNode = (ClassDefNode) node;
                BodyNode bodyNode = (BodyNode) classDefNode.make;

                List<MethodDeclareNode> methodDeclare = classDefNode.methods;

                for (MethodDeclareNode methodDeclareNode : methodDeclare) {
                    this.processSingletonNode(context,classDefNode,methodDeclareNode);
                }
            }
            case INLINE_DEFINITION -> { // global
                InlineDeclareNode inlineDeclareNode  = (InlineDeclareNode) node;

                Function function = new Function(context, null, inlineDeclareNode);
            }
            case METHOD_DEFINITION -> { // only possible in class
                MethodDeclareNode methodDeclareNode = (MethodDeclareNode) node;

                ClassDefNode classDefNode = null;
                if(parent != null && parent.getNodeType() == NodeType.CLASS_DEFINITION) {
                    classDefNode = (ClassDefNode) parent;
                }

                Function function = new Function(context, classDefNode, methodDeclareNode);

                BodyNode bodyNode = (BodyNode) methodDeclareNode.body;
                for (Node statement : bodyNode.statements) {
                    processSingletonNode(context, parent, statement);
                }
            }

            case CALL -> {

            }
        }
    }

    void error(String type, String message) {
        LanguageException languageException = new LanguageException(
                LanguageException.Type.COMPILER,
                type + " Error", message
        );

        SYSTEM_LOGGER.fail("", "", languageException);
    }

}

