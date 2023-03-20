package language.backend.compiler.bytecode.ir.impl;

import language.backend.compiler.bytecode.ir.CompressionMethod;
import language.backend.compiler.bytecode.ir.huffman.HuffmanDecoder;
import language.backend.compiler.bytecode.ir.huffman.HuffmanEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CombinedMethod implements CompressionMethod {
    private final HuffmanEncoder encoder = new HuffmanEncoder();
    private final HuffmanDecoder decoder = new HuffmanDecoder();

    @Override
    public byte[] compress(byte[] data) {
        try {
            return compress0(encoder.encode(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decompress(byte[] data) {
        try {
            return decoder.decode(decompress0(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] compress0(byte[] data) {
        try {
            Deflater deflater = new Deflater(9);


            deflater.setInput(data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

            deflater.finish();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();

            return outputStream.toByteArray();
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public byte[] decompress0(byte[] data) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();

            inflater.reset();

            return outputStream.toByteArray();
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
