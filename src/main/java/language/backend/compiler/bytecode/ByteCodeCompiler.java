package language.backend.compiler.bytecode;

import dtool.DefaultDtoolRuntime;
import language.backend.compiler.AbstractCompiler;
import language.backend.compiler.bytecode.types.objects.ClassObjectType;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.frontend.parser.nodes.Node;
import language.vm.library.LibraryClassLoader;
import language.vm.library.NativeContext;

import java.util.HashMap;
import java.util.List;

public class ByteCodeCompiler extends AbstractCompiler {

    private ByteCode byteCode;

    @Override
    public byte[] compile(String source, List<Node> ast) {
        Compiler compiler = new Compiler(FunctionType.SOURCE, source, ClassObjectType.EMPTY);
        compiler.chunk().globals = new HashMap<>();
        compiler.chunk().globals.putAll(LibraryClassLoader.LIBRARY_TYPES);
        compiler.chunk().globals.putAll(NativeContext.GLOBAL_TYPES);

        byteCode = compiler.compileBlock(ast);

        return DefaultDtoolRuntime.COMPRESSOR
                .compress(byteCode.dumpBytes());
    }

    @Override
    public ByteCode getByteCode() {
        return byteCode;
    }
}
