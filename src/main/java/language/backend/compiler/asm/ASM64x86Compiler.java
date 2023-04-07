package language.backend.compiler.asm;

import language.backend.compiler.AbstractCompiler;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.InlineDeclareNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import language.frontend.parser.nodes.expressions.CallNode;
import language.frontend.parser.nodes.expressions.UseNode;
import language.frontend.parser.nodes.extra.CompilerNode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASM64x86Compiler extends AbstractCompiler {
    private static final String TAB = "    ";
    private static final String COMMENT = ";";

    public final Map<String, byte[]> globalStrings = new HashMap<>();
    public final Map<String, String> labelStrings = new HashMap<>();

    public final Map<String, Integer> functionMapping = new HashMap<>();
    public final Map<String, Integer> methodMapping = new HashMap<>();

    private static final boolean DEBUG = true;
    private static final boolean SELF = false;
    private static final boolean REG_PARAM = false;

    private int methodId, functionId;

    private StringBuilder context = new StringBuilder();

    @Override
    public byte[] compile(String source, List<Node> ast) {
        ast.stream().map(Node::optimize).forEach(this::processSingletonNode);

        // elf header
        StringBuilder header = new StringBuilder();
        ElfHeader elfHeader = new ElfHeader(DEBUG, SELF, "");
        elfHeader.appendHeader(header);
        elfHeader.appendSectionText(header, """
                    call %s
                    mov rdi, rax
                    mov rax, 60
                    syscall
                    \n
                """.formatted("MAIN FUNCTION HERE"), context.toString()); // TODO FIND MAIN
        elfHeader.appendSectionData(header, this);
        elfHeader.appendSectionSections(header);
        elfHeader.appendSectionDebug(header, this);
        context = header;

        return context.toString().getBytes(StandardCharsets.UTF_8);
    }

    public void processSingletonNode(final Node node) {
        NodeType nodeType = node.getNodeType();

        //System.out.println(nodeType);

        switch (nodeType) {
            case COMPILER -> {
                CompilerNode compilerNode = (CompilerNode) node;
                emit("%s compiler statement", COMMENT);
                for (Token token : compilerNode.getCode()) {
                    emit(token.asString());
                }
            }

            case INLINE_DEFINITION -> {
                final InlineDeclareNode declareNode = (InlineDeclareNode) node;

            }
            case METHOD_DEFINITION -> {
                final MethodDeclareNode declareNode = (MethodDeclareNode) node;
            }

            case CALL -> {
                CallNode callNode = (CallNode) node;

            }
            case USE -> {
                UseNode useNode = (UseNode) node;

            }

            case ATTRIBUTE_ASSIGN -> {}
            case ATTRIBUTE_ACCESS -> {}
            case ATTRIBUTE -> {}
            case ASSERT -> {}
            case BYTES -> {}
            case BASE_FUNCTION -> {}
            case BREAK -> {}
            case BIN_OP -> {}
            case BOOLEAN -> {}
            case CLASS_INSTANCE -> {}
            case CLASS_PLATE -> {}
            case C_METHOD -> {}
            case CLASS_DEFINITION -> {
                ClassDefNode classDefNode = (ClassDefNode) node;
                for (MethodDeclareNode method : classDefNode.methods) {
                    processSingletonNode(method);
                }
            }

            case CLASS_ACCESS -> {}
            case CONTINUE -> {}
            case DE_REF -> {}
            case DYNAMIC_ASSIGN -> {}
            case MAP -> {}
            case DECORATOR -> {}
            case ENUM -> {}
            case ENUM_CHILD -> {}
            case FUNCTION -> {}
            case FOR -> {}
            case GENERIC -> {}
            case IMPORT -> {}
            case ITERATOR -> {}
            case LIBRARY -> {}
            case LIST -> {}
            case NULL -> {}
            case NUMBER -> {}
            case PATTERN -> {}
            case PASS -> {}
            case QUERY -> {}
            case REFERENCE -> {}
            case RETURN -> {}
            case RES -> {}
            case STRING -> {}
            case SWITCH -> {}
            case SPREAD -> {}
            case TYPE_DEFINITION -> {}
            case UNARY_OPERATION -> {}
            case VAR_ASSIGNMENT -> {}
            case VALUE -> {}
            case VAR_ACCESS -> {}
            case VAR -> {}
            case WHILE -> {}
            case BODY -> {}
            case SCOPE -> {}
            case LET -> {}
            case THROW -> {}
            case DROP -> {}
            case DESTRUCT -> {}
            case CAST -> {}
            case PACKAGE -> {}

            case MACRO_DEFINITION -> {}

        }
    }

    public void emit(final String text, Object... varargs) {
        this.context.append(text.formatted(varargs)).append('\n');
    }

}
