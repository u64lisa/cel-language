package language.backend.compiler.llvm;

import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.definitions.InlineDeclareNode;
import language.frontend.parser.nodes.definitions.MethodDeclareNode;
import language.frontend.parser.nodes.values.NumberNode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryStack.*;

public class LLVMCompilerUtils {

    public static long makeFunctionType(final LLVMTypeLookup llvmTypeLookup, Node node) {
        if (node instanceof MethodDeclareNode methodDeclareNode) {
            return makeFunctionType(llvmTypeLookup, methodDeclareNode);
        }
        if (node instanceof InlineDeclareNode inlineDeclareNode) {
            return makeFunctionType(llvmTypeLookup, inlineDeclareNode);
        }
        throw new IllegalStateException("Unexpected node for function type creation: " + node.getNodeType());
    }

    public static long makeFunctionType(final LLVMTypeLookup llvmTypeLookup, InlineDeclareNode inlineDeclareNode) {
        if(inlineDeclareNode.returnType.size() > 1) {
            System.out.println("Not implemented");
            System.exit(1);
        }

        long returnType = llvmTypeFromString(llvmTypeLookup, inlineDeclareNode.returnType.get(0));
        long[] argumentTypeArray = inlineDeclareNode.argumentTypes.stream()
                .map(Token::asString).mapToLong((element) -> llvmTypeFromString(llvmTypeLookup, element))
                .toArray();

        try(final MemoryStack stack = stackPush()) {
            PointerBuffer argumentTypes = stack.mallocPointer(argumentTypeArray.length);
            for(int i = 0; i < argumentTypeArray.length; i++) {
                argumentTypes.put(i, argumentTypeArray[i]);
            }

            return LLVMFunctionType(returnType, argumentTypes, false);
        }
    }
    public static long makeFunctionType(final LLVMTypeLookup llvmTypeLookup, MethodDeclareNode methodDeclareNode) {
        if(methodDeclareNode.returnType.size() > 1) {
            System.out.println("Not implemented");
            System.exit(1);
        }

        long returnType = llvmTypeFromString(llvmTypeLookup, methodDeclareNode.returnType.get(0));
        long[] argumentTypeArray = methodDeclareNode.argumentTypes.stream()
                .map(Token::asString).mapToLong((element) -> llvmTypeFromString(llvmTypeLookup, element))
                .toArray();

        try(final MemoryStack stack = stackPush()) {
            PointerBuffer argumentTypes = stack.mallocPointer(argumentTypeArray.length);
            for(int i = 0; i < argumentTypeArray.length; i++) {
                argumentTypes.put(i, argumentTypeArray[i]);
            }

            return LLVMFunctionType(returnType, argumentTypes, false);
        }
    }

    public static long llvmTypeFromString(final LLVMTypeLookup llvmTypeLookup, String type) {
        double a = 0.5;

        //TODO: translate using typelookup

        type = llvmTypeLookup.getType(type).name;
        System.out.println(type + "!!!!");

        return switch (type) {
            case "void" -> LLVMVoidType();
            case "any" -> LLVMPointerType(LLVMVoidType(), 0); //TODO: what is any ? xD an object?

            case "i8" -> LLVMInt8Type();
            case "i16" -> LLVMInt16Type();
            case "i32" -> LLVMInt32Type();

            case "i64", "l64" -> LLVMInt64Type();

            case "f32" -> LLVMFloatType();
            case "f64" -> LLVMDoubleType();
            default -> LLVMPointerType(LLVMVoidType(), 0);
        };
    }

    public static String returnTypeName(final Node node) {
        switch (node.getNodeType()) {
            case INLINE_DEFINITION -> {
                InlineDeclareNode inlineDeclareNode = (InlineDeclareNode) node;
                return inlineDeclareNode.returnType.get(0);
            }
            case METHOD_DEFINITION -> {
                MethodDeclareNode methodDeclareNode = (MethodDeclareNode) node;
                return methodDeclareNode.returnType.get(0);
            }
            default -> {
                return null;
            }
        }
    }

    public static long buildConstant(final LLVMTypeLookup typeLookup, final NumberNode numberNode, final String type) {
        long llvmType = llvmTypeFromString(typeLookup, type);
        boolean isFloating = llvmType == LLVMFloatType() || llvmType == LLVMDoubleType();
        long result;
        if(!isFloating) {
            result = LLVMCore.LLVMConstInt(llvmType, (long)numberNode.val, false);
        } else {
            //TODO: implement float constant
            result = 0;
        }

        return result;
    }
}
