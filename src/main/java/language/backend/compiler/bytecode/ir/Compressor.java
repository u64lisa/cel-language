package language.backend.compiler.bytecode.ir;

import language.backend.compiler.bytecode.ir.impl.CombinedMethod;
import language.backend.compiler.bytecode.ir.impl.InDeFlatorMethod;
import language.backend.compiler.bytecode.ir.impl.StreamMethod;
import language.backend.compiler.bytecode.ir.impl.HuffManMethod;

public class Compressor {

    private final CompressionMethod[] methods = {
            new InDeFlatorMethod(), // 223.9kb
            new StreamMethod(), // 261.2kb
            new HuffManMethod(), // 756.5kb
            new CombinedMethod() // 251.kb
    };

    public CompressionMethod compressionMethod = methods[3];

    public byte[] decompress(final byte[] bytes) {
        return compressionMethod.decompress(bytes);
    }

    public byte[] compress(final byte[] bytes) {
        return compressionMethod.compress(bytes);
    }

}
