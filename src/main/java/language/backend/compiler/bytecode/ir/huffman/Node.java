package language.backend.compiler.bytecode.ir.huffman;

public class Node implements Comparable<Node> {

	public int freq;
	public Short datum;
	public Node left;
	public Node right;

	public Node(int freq, Short datum, Node left, Node right) {
		this.freq = freq;
		this.datum = datum;
		this.left = left;
		this.right = right;
	}

	public Node(int freq, Node left, Node right) {
		this(freq, null, left, right);
	}

	public Node(int freq, Short datum) {
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
	public int compareTo(Node obj) {
		if (this.freq > obj.freq) {
			return 1;
		} else if (this.freq < obj.freq) {
			return -1;
		} else {
			return 0;
		}
	}
}
