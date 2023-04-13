package language.backend.compiler.llvm;

import org.lwjgl.llvm.LLVMCore;

import java.util.HashMap;

import static org.lwjgl.llvm.LLVMCore.LLVMDumpModule;
import static org.lwjgl.llvm.LLVMCore.LLVMModuleCreateWithName;

public class CompilerContext {
    public final String moduleName;
    public final long moduleHandle;

    public final HashMap<String, Function> staticFunctions;

    public CompilerContext(final String moduleName) {
        try {
            LLVMCore.getLibrary();
        } catch (UnsatisfiedLinkError linkError) {
            throw new RuntimeException("Please configure the LLVM shared libraries path with:\n \t-Dorg.lwjgl.llvm.libname=<LLVM shared library path> or\n \t-Dorg.lwjgl.librarypath=<path that contains LLVM shared libraries>");
        }

        this.moduleName = moduleName;
        moduleHandle = LLVMModuleCreateWithName(moduleName);

        staticFunctions = new HashMap<>();
    }

    public void print() {
        LLVMDumpModule(moduleHandle);
    }
}
