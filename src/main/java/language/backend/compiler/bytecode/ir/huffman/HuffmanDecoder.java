package language.backend.compiler.bytecode.ir.huffman;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HuffmanDecoder {

	public byte[] decode(final byte[] inData) throws IOException {
		BitInputStream in = new BitInputStream(inData);
		BitOutputStream out = new BitOutputStream();

		int header = in.readBits(32);
		if (header != 1846) {
			throw new IllegalArgumentException("Not a huffman file");
		}

		int numCodes = in.readBits(32);
		Map<Short, Integer> table = makeTable(in, numCodes);

		HuffmanTree ht = new HuffmanTree(table);
		ht.decode(in, out);

		in.close();
		out.close();

		return out.getBytes();
	}

	private Map<Short, Integer> makeTable(BitInputStream in, int n) {
		Map<Short, Integer> m = new HashMap<Short, Integer>();
		for (int i = 0; i < n; i++) {
			addEntry(in, m);
		}
		return m;
	}

	private void addEntry(BitInputStream in, Map<Short, Integer> m) {
		short key = (short) in.readBits(16);
		int val = in.readBits(32);
		m.put(key, val);
	}
}
