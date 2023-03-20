package language.backend.compiler.bytecode.ir;

public interface CompressionMethod {

    byte[] compress(final byte[] data);
    byte[] decompress(final byte[] data);

}
