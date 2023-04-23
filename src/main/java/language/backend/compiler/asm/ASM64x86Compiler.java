package language.backend.compiler.asm;

import language.backend.compiler.AbstractCompiler;
import language.backend.compiler.asm.inst.Parameter;
import language.backend.compiler.asm.types.ASMTypeLookup;
import language.backend.compiler.bytecode.TypeLookup;
import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.InlineDeclareNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import language.frontend.parser.nodes.expressions.BodyNode;
import language.frontend.parser.nodes.expressions.CallNode;
import language.frontend.parser.nodes.expressions.UseNode;
import language.frontend.parser.nodes.extra.CompilerNode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASM64x86Compiler extends AbstractCompiler {

    private final ASMTypeLookup typeLookup = new ASMTypeLookup();

    public final String asmFormat = "asm_%x:";
    public final String inlineFormat = "inline_%x:";
    public final String methodFormat = "method_%x:";
    public final String origNameFormat = "original name '%s'";

    public final Map<String, byte[]> globalStrings = new HashMap<>();
    public final Map<String, String> labelStrings = new HashMap<>();

    public final Map<String, Integer> inlineMapping = new HashMap<>();
    public final Map<String, Integer> methodMapping = new HashMap<>();
    public final Map<String, Integer> asmMapping = new HashMap<>();

    private static final boolean DEBUG = true;
    private static final boolean SELF = false;
    private static final boolean REG_PARAM = false;

    private int methodId, inlineId, asmCodeId;

    private ASMContext context = new ASMContext();

    @Override
    public byte[] compile(String source, List<Node> ast) {
        ast.removeIf(node -> node.getNodeType() == NodeType.PACKAGE);

        for (Node node : ast.stream().map(Node::optimize).toList()) {
            // todo wrap node in procedure
            this.processSingletonNode(node, new ASMProcedure());
        }

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
                """.formatted("MAIN FUNCTION HERE"), context.getStringBuilder().toString()); // TODO FIND MAIN
        elfHeader.appendSectionData(header, this);
        elfHeader.appendSectionSections(header);
        elfHeader.appendSectionDebug(header, this);

        return header
                .toString().getBytes(StandardCharsets.UTF_8);
    }

    public void processSingletonNode(final Node node, ASMProcedure asmProcedure) {
        NodeType nodeType = node.getNodeType();

        //System.out.println(node.getNodeType());
        switch (nodeType) {
            case BODY -> {
                BodyNode bodyNode = (BodyNode) node;

                for (Node statement : bodyNode.statements) {
                    // todo create new procedure
                    this.processSingletonNode(statement, new ASMProcedure());
                }
            }
            case COMPILER -> {
                CompilerNode compilerNode = (CompilerNode) node;
                context.aC(origNameFormat.formatted(compilerNode.getName().asString()));

                context.aH(asmFormat.formatted(asmCodeId),
                        "inline compiler");

                asmMapping.put(compilerNode.getName().asString(), asmCodeId);

                context.aI("push RBP");
                context.aI("mov RBP, RSP");
                context.aI("mov eax, 0", "breaks dependency chain");
                context.aI("sub RSP, 0x%x".formatted(asmProcedure.getStackSize()), "stack size");
                context.nl();

                for (Token token : compilerNode.getCode()) {
                    context.aI(token.asString());
                }

                context.nl();
                context.aI("mov RSP, RBP");
                context.aI("pop RBP");
                context.aI("ret");
                context.nl();

                asmCodeId++;
            }

            case INLINE_DEFINITION -> {
                final InlineDeclareNode declareNode = (InlineDeclareNode) node;
                context.aC(origNameFormat.formatted(declareNode.name.asString()))
                        .aH(inlineFormat.formatted(inlineId),
                        "inline definition");

                inlineMapping.put(declareNode.name.asString(), inlineId);

                context.aI("push RBP")
                        .aI("mov RBP, RSP")
                        .nl()
                        .aI("mov eax, 0", "breaks dependency chain")
                        .aI("sub RSP, 0x%x".formatted(asmProcedure.getStackSize()), "stack size")
                        .nl()
                        .aCT("parameters");

                AsmRegister[] registers = new AsmRegister[]{
                        AsmRegister.DX, AsmRegister.DI, AsmRegister.CX, AsmRegister.SI,
                        AsmRegister.R8, AsmRegister.R9
                };

                int offset = 16;
                for (int i = 0; i < declareNode.argumentNames.size(); i++) {
                    Token name = declareNode.argumentNames.get(i);
                    Token type = declareNode.argumentTypes.get(i);

                    Parameter param = typeLookup.validateRef(name, type);

                    if (REG_PARAM && i < registers.length) {
                        context.aI("mov %s, &s".formatted(
                                AsmUtils.getParamValue(param, asmProcedure),
                                registers[i].toString(param)
                        ));
                    } else {
                        String regName = AsmRegister.AX.toString(param);
                        int size = AsmUtils.getTypeSize(param);

                        context.aI("mov %s, %s [RBP + 0x%x]".formatted(
                            regName,
                            AsmUtils.getPointerName(size),
                            offset
                        ));
                        context.aI("mov %s, %s".formatted(
                            AsmUtils.getParamValue(param, asmProcedure),
                            regName
                        ));


                        offset += (size >> 3);
                    }
                }

                for (Node child : declareNode.getChildren()) {
                    // todo create new procedure
                    this.processSingletonNode(child, new ASMProcedure());
                }

                context.nl()
                        .aCT("closing")
                        .aI("mov RSP, RBP")
                        .aI("pop RBP")
                        .aI("ret")
                        .nl();

                inlineId++;
            }
            case METHOD_DEFINITION -> {
                final MethodDeclareNode declareNode = (MethodDeclareNode) node;
                context.aC(origNameFormat.formatted(declareNode.name.asString()));

                context.aH(methodFormat.formatted(methodId),
                        "method definition");

                inlineMapping.put(declareNode.name.asString(), methodId);

                context.aI("push RBP");
                context.aI("mov RBP, RSP");
                context.aI("mov eax, 0", "breaks dependency chain");
                context.aI("sub RSP, 0x%x".formatted(asmProcedure.getStackSize()), "stack size");
                context.nl();

                //

                context.nl();
                context.aI("mov RSP, RBP");
                context.aI("pop RBP");
                context.aI("ret");
                context.nl();

                methodId++;
            }

            case CALL -> {
                CallNode callNode = (CallNode) node;

                System.out.println(callNode.getNodeToCall());
                System.out.println(callNode.getArgNodes());
                AsmRegister[] regs = {
                        AsmRegister.DI, AsmRegister.SI, AsmRegister.DX, AsmRegister.CX,
                        AsmRegister.R8, AsmRegister.R9
                };


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
                    processSingletonNode(method, new ASMProcedure());
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

}
