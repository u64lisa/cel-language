package language.backend.compiler.bytecode.ir.impl;

import language.backend.compiler.bytecode.ir.CompressionMethod;
import language.backend.compiler.bytecode.ir.huffman.HuffmanDecoder;
import language.backend.compiler.bytecode.ir.huffman.HuffmanEncoder;

import java.io.IOException;

public class HuffManMethod implements CompressionMethod {

    private final HuffmanEncoder encoder = new HuffmanEncoder();
    private final HuffmanDecoder decoder = new HuffmanDecoder();

    @Override
    public byte[] compress(byte[] data) {
        try {
            return encoder.encode(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decompress(byte[] data) {
        try {
            return decoder.decode(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
