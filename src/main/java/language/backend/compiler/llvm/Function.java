package language.backend.compiler.llvm;

import language.frontend.parser.nodes.definitions.ClassDefNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;

import static org.lwjgl.llvm.LLVMCore.*;
public class Function {
    public final CompilerContext context;
    public final ClassDefNode classDefNode;
    public final MethodDeclareNode methodDeclareNode;
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
    public Function(final CompilerContext context, final ClassDefNode classDefNode, final MethodDeclareNode methodDeclareNode) {
        this.context = context;
        this.classDefNode = classDefNode;
        this.methodDeclareNode = methodDeclareNode;

        String name = null;
        if(classDefNode == null) {
            name = methodDeclareNode.name.asString();
        } else {
            name = String.format(methodDeclareNode.asString().equals("main") ? "main" : String.format("%s_%s",
                    classDefNode.className.asString(), methodDeclareNode.name.asString()));
        }

        this.name = name;

        functionTypeHandle = LLVMCompilerUtils.makeFunctionType(methodDeclareNode);
        functionHandle = LLVMAddFunction(context.moduleHandle, name, functionTypeHandle);
    }

    public void createBody() {
        builderHandle = LLVMCreateBuilder();
        entryBlockHandle = LLVMAppendBasicBlock(functionHandle, "entry");
    }
}
