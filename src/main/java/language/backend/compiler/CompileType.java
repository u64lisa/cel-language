package language.backend.compiler;

import language.backend.compiler.asm.ASM64x86Compiler;
import language.backend.compiler.bytecode.ByteCodeCompiler;

public enum CompileType {

    // asm
    ASM_64x86(ASM64x86Compiler.class, true),

    // ir
    CUSTOM_IR(ByteCodeCompiler.class, false);

    private final Class<? extends AbstractCompiler> compiler;
    private final boolean asm;

    CompileType(Class<? extends AbstractCompiler> compiler, boolean asm) {
        this.compiler = compiler;
        this.asm = asm;
    }

    @SuppressWarnings("deprecation")
    public AbstractCompiler constructCompiler() {

        try {
            return this.compiler.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isAsm() {
        return asm;
    }
}
