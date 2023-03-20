package language.backend.compiler.bytecode.ir.huffman;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HuffmanEncoder {
    private Map<Short, Integer> createFrequencyMap(byte[] data) throws IOException {
        BitInputStream in = new BitInputStream(data);
        Map<Short, Integer> m = new HashMap<Short, Integer>();
        for (short c = 0; (c = (short) in.readBits(8)) != -1; ) {
            if (m.containsKey(c)) {
                m.put(c, (m.get(c) + 1));
            } else {
                m.put(c, 1);
            }
        }

        return m;
    }

    public byte[] encode(final byte[] inData) throws IOException {
        Map<Short, Integer> m = createFrequencyMap(inData);

        BitInputStream in = new BitInputStream(inData);
        BitOutputStream out = new BitOutputStream();

        out.writeBits(1846, 32);
        out.writeBits(m.size(), 32);

        m.forEach((k, v) -> {
            out.writeBits((int) k, 16);
            out.writeBits(v, 32);
        });

        HuffmanTree ht = new HuffmanTree(m);
        ht.encode(in, out);

        in.close();
        out.close();

        return out.getBytes();
    }
}
