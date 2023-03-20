package language.backend.compiler.bytecode.ir.huffman;

import java.io.*;

public class BitInputStream {
    private ByteArrayInputStream input;
    private int digits;     // next set of digits (buffer)
    private int cursor;     // how many digits from buffer have been used

    private static final int BYTE_SIZE = 8;  // digits per byte

    public BitInputStream(byte[] data) throws IOException {
        input = new ByteArrayInputStream(data);
        nextByte();
    }

    public boolean hasBits() {
        return digits == -1;
    }

    public int readBit() {
        // if at eof, return -1
        if (digits == -1) {
            return -1;
        }
        int result = (digits & (1 << cursor)) >> cursor;
        cursor--;
        if (cursor < 0) {
            nextByte();
        }
        return result;
    }

    public int readBits(int n) {
        int ret = 0;
        for (int i = n - 1; i >= 0; i--) {
            int bit = readBit();
            if (bit == -1) {
                return -1;
            }
            ret = ret | (bit << i);
        }
        return ret;
    }

    private void nextByte() {
        digits = input.read();
        cursor = BYTE_SIZE - 1;
    }

    public void close() {
        try {
            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    protected void finalize() {
        close();
    }
}
