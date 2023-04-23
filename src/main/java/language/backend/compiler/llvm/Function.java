package language.backend.compiler.llvm;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.InlineDeclareNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;

import static org.lwjgl.llvm.LLVMCore.*;
public class Function {
    public final CompilerContext context;
    public final ClassDefNode classDefNode;
    public final Node node;
    public final String name;
    public final long functionTypeHandle;
    public final long functionHandle;
    private long builderHandle;
    private long entryBlockHandle;

    public long getBuilderHandle() {
        return builderHandle;
    }

    public long getEntryBlockHandle() {
        return entryBlockHandle;
    }

    private static String computeFunctionName(Node node) {
        return switch (node.getNodeType()) {
            case METHOD_DEFINITION -> {
                MethodDeclareNode methodDeclareNode = (MethodDeclareNode) node;
                yield methodDeclareNode.name.asString();
            }
            case INLINE_DEFINITION -> {
                InlineDeclareNode inlineDeclareNode = (InlineDeclareNode) node;
                yield inlineDeclareNode.name.asString();
            }
            default -> throw new RuntimeException("Unsupported node type: " + node.getNodeType());
        };
    }

    public Function(final CompilerContext context, final ClassDefNode classDefNode, final Node node) {
        this.context = context;
        this.classDefNode = classDefNode;
        this.node = node;

        String nodeName = computeFunctionName(node);
        String name = null;
        if(classDefNode == null) {
            name = nodeName;
        } else {
            name = String.format(nodeName.equals("main") ? "main" : String.format("%s_%s",
                    classDefNode.className.asString(), nodeName));
        }

        this.name = name;

        functionTypeHandle = LLVMCompilerUtils.makeFunctionType(context.getTypeLookup(), node);
        functionHandle = LLVMAddFunction(context.moduleHandle, name, functionTypeHandle);

        if(!context.staticFunctions.containsKey(this.name)) {
            context.staticFunctions.put(this.name, this);
        }
    }

    public void createBody() {
        builderHandle = LLVMCreateBuilder();
        entryBlockHandle = LLVMAppendBasicBlock(functionHandle, "entry");
    }
}
