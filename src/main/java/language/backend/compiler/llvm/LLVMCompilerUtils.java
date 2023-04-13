package language.backend.compiler.llvm;

import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryStack.*;

public class LLVMCompilerUtils {
    public static long makeFunctionType(MethodDeclareNode methodDeclareNode) {
        if(methodDeclareNode.returnType.size() > 1) {
            System.out.println("Not implemented");
            System.exit(1);
        }

        long returnType = llvmTypeFromString(methodDeclareNode.returnType.get(0));
        long[] argumentTypeArray = methodDeclareNode.argumentTypes.stream()
                .map(Token::asString).mapToLong(LLVMCompilerUtils::llvmTypeFromString)
                .toArray();

        try(final MemoryStack stack = stackPush()) {
            PointerBuffer argumentTypes = stack.mallocPointer(argumentTypeArray.length);
            for(int i = 0; i < argumentTypeArray.length; i++) {
                argumentTypes.put(i, argumentTypeArray[i]);
            }

            return LLVMFunctionType(returnType, argumentTypes, false);
        }
    }

    public static long llvmTypeFromString(String type) {
        //TODO: handle typedef stuff. Unfortunately, it is not handled elsewhere.
        return switch (type) {
            case "void" -> LLVMVoidType();
            case "any" -> LLVMPointerType(LLVMVoidType(), 0); //TODO: what is any ? xD an object?
            case "byte" -> LLVMInt8Type();
            case "short" -> LLVMInt16Type();
            case "int" -> LLVMInt32Type();
            case "long" -> LLVMInt64Type();
            default -> LLVMPointerType(LLVMVoidType(), 0);
        };
    }
}
