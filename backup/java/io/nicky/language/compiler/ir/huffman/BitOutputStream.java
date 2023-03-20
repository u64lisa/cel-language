package language.backend.compiler.bytecode.ir.huffman;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BitOutputStream {

    private ByteArrayOutputStream buffer;

    private int digits;     // a buffer used to build up next set of digits
    private int cursor;     // our current position in the buffer.
    private boolean debug;  // set to true to write ASCII 0s and 1s rather than
                            // bits

    private static final int BYTE_SIZE = 8; // digits per byte

    public BitOutputStream(boolean debug) throws IOException {
        buffer = new ByteArrayOutputStream();

        this.debug = debug;
        digits = 0;
        cursor = BYTE_SIZE - 1;
    }

    public BitOutputStream() throws IOException {
        this(false);
    }

    public void writeBit(int bit) {
        if (bit < 0 || bit > 1) {
            throw new IllegalArgumentException("Illegal bit: " + bit);
        } else if (debug) {
            buffer.write(bit);
        } else {
            digits += bit << cursor;
            cursor--;
            if (cursor < 0) {
                flush();
            }
        }
    }

    public void writeBits(int bits, int n) {
        for (int i = n-1; i >= 0; i--) {
            writeBit((bits >>> i) % 2);
        }
    }

    private void flush() {
        if (cursor == BYTE_SIZE - 1) {
            return;
        }
        buffer.write(digits);
        digits = 0;
        cursor = BYTE_SIZE - 1;
    }

    public void close() {
        if (cursor >= 0) {
            flush();
        }
        try {
            buffer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void finalize() {
        close();
    }

    public byte[] getBytes() {
        return buffer.toByteArray();
    }
}
