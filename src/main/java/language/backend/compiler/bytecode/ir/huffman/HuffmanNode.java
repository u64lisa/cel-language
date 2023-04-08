package language.backend.compiler.bytecode.ir.huffman;

public class HuffmanNode implements Comparable<HuffmanNode> {

	public int freq;
	public Short datum;
	public HuffmanNode left;
	public HuffmanNode right;

	public HuffmanNode(int freq, Short datum, HuffmanNode left, HuffmanNode right) {
		this.freq = freq;
		this.datum = datum;
		this.left = left;
		this.right = right;
	}

	public HuffmanNode(int freq, HuffmanNode left, HuffmanNode right) {
		this(freq, null, left, right);
	}

	public HuffmanNode(int freq, Short datum) {
		this(freq, datum, null, null);
	}

	public String toStringH(String soFar) {
		if (datum != null) {
			soFar += "Frequency: " + freq + "; Datum: " + (char) (short) datum + "\n";
		} else {
			soFar += "[ " + freq + " ]\n";
			soFar += left.toStringH("");
			soFar += right.toStringH("");
		}

		return soFar;
	}

	public String toString() {
		return toStringH("");
	}

	@Override
	public int compareTo(HuffmanNode obj) {
		if (this.freq > obj.freq) {
			return 1;
		} else if (this.freq < obj.freq) {
			return -1;
		} else {
			return 0;
		}
	}
}
