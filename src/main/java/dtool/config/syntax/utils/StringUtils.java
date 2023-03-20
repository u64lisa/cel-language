package dtool.config.syntax.utils;

import java.lang.reflect.Array;
import java.util.List;

public final class StringUtils {
	public static String join(CharSequence separator, List<?> list) {
		if (list == null) return null;
		return join(separator, list.toArray());
	}
	
	public static long parseLong(String value) {
		if (value.startsWith("0x")) {
			return Long.parseLong(value.substring(2, value.length() - 1), 16);
		} else {
			return Long.parseLong(value.substring(0, value.length() - 1));
		}
	}
	
	public static int parseInteger(String value) {
		if (value.startsWith("0x")) {
			return Integer.parseInt(value.substring(2), 16);
		} else {
			return Integer.parseInt(value);
		}
	}
	
	public static String join(CharSequence separator, Object array) {
		if(array == null) return null;
		
		try {
			int length = Array.getLength(array);
			
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < length; i++) {
				sb.append(separator).append(Array.get(array, i));
			}
			
			if(length > 0) {
				sb.delete(0, separator.length());
			}
			
			return sb.toString();
		} catch(IllegalArgumentException e) {
			return null;
		}
	}
	
	public static String toStringCustomBase(long value, String base) {
		return toStringCustomBase(value, base, true);
	}
	
	public static String toStringCustomBase(long value, String base, boolean isAlphabetical) {
		if(base == null) throw new NullPointerException();
		if(base.length() < 2) throw new IllegalArgumentException();
		
		StringBuilder sb = new StringBuilder();
		int length = base.length();
		int offset = isAlphabetical ? 0:1;
		
		// Allow negative values to be outputed. Will always turn a negative value positive
		// because the smallest allowed base is 2 and that base and all bases greater than 2
		// will remove the signed bit from the long value.
		if(value < 0) {
			long v = Long.remainderUnsigned(value, length);
			sb.append(base.charAt((int)v));
			value = Long.divideUnsigned(value - v, length) - offset;
		}
		
		while(true) {
			long v = value % length;
			sb.insert(0, base.charAt((int)v));
			
			if(value >= length) {
				value = ((value - v) / length) - offset;
			} else {
				break;
			}
		}
		
		return sb.toString();
	}
	
	public static String unescapeString(String string) {
		if(string == null) return null;
		
		StringBuilder sb = new StringBuilder();
		boolean escape = false;
		
		for(int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			
			if(escape) {
				escape = false;
				
				switch(c) {
					case '\'': case '\"': case '\\': sb.append(c); break;
					case '0': sb.append('\0'); break;
					case 'r': sb.append('\r'); break;
					case 'n': sb.append('\n'); break;
					case 'b': sb.append('\b'); break;
					case 't': sb.append('\t'); break;
					case 'x': {
						if(i + 3 > string.length()) {
							throw new MalformedEscapeException("(index:" + i + ") Not enough characters for '\\x..' escape.");
						}
						
						String hex = string.substring(i + 1, i + 3);
						
						try {
							sb.append((char)(int)Integer.valueOf(hex, 16));
						} catch(NumberFormatException e) {
							throw new MalformedEscapeException("(index:" + i + ") Invalid escape '\\x" + hex + "'");
						}
						
						i += 2;
						break;
					}
					case 'u': {
						if(i + 5 > string.length()) {
							throw new MalformedEscapeException("(index:" + i + ") Not enough characters for '\\u....' escape.");
						}
						
						String hex = string.substring(i + 1, i + 5);
						
						try {
							sb.append((char)(int)Integer.valueOf(hex, 16));
						} catch(NumberFormatException e) {
							throw new MalformedEscapeException("(index:" + i + ") Invalid escape '\\u" + hex + "'");
						}
						
						i += 4;
						break;
					}
					
					default: {
						throw new MalformedEscapeException("(index:" + i + ") Invalid character escape '\\" + c + "'");
					}
				}
			} else if(c == '\\') {
				escape = true;
			} else {
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	
	public static String escapeString(String string) {
		if(string == null) return null;
		
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);

			switch (c) { // Normal escapes
				case '\r' -> {
					sb.append("\\r");
					continue;
				}
				case '\n' -> {
					sb.append("\\n");
					continue;
				}
				case '\b' -> {
					sb.append("\\b");
					continue;
				}
				case '\t' -> {
					sb.append("\\t");
					continue;
				}
				case '\'' -> {
					sb.append("\\\'");
					continue;
				}
				case '\"' -> {
					sb.append("\\\"");
					continue;
				}
				case '\\' -> {
					sb.append("\\\\");
					continue;
				}
			}
			
			if(c > 0xff) { // Unicode
				sb.append("\\u").append(toHexString(c, 4));
				continue;
			}
			
			if(Character.isISOControl(c)) { // Control character
				sb.append("\\x").append(toHexString(c, 2));
				continue;
			}
			
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	public static String regexEscape(String string) {
		if(string == null) return null;
		
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);

			switch (c) { // Normal escapes
				case '\0' -> {
					sb.append("\\0");
					continue;
				}
				case '\n' -> {
					sb.append("\\n");
					continue;
				}
				case '\r' -> {
					sb.append("\\r");
					continue;
				}
				case '\t' -> {
					sb.append("\\t");
					continue;
				}
				case '\\' -> {
					sb.append("\\\\");
					continue;
				}
				case '^', '$', '?', '|', '*', '/', '+', '.', '(', ')', '[', ']', '{', '}' -> {
					sb.append("\\").append(c);
					continue;
				}
			}
			
			if(c > 0xff) { // Unicode
				sb.append("\\u").append(toHexString(c, 4));
				continue;
			}
			
			if(Character.isISOControl(c)) { // Control character
				sb.append("\\x").append(toHexString(c, 2));
				continue;
			}
			
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	public static int countInstances(CharSequence string, char c) {
		if(string != null) {
			int count = 0;
			
			for(int i = 0; i < string.length(); i++) {
				count += (string.charAt(i) == c) ? 1:0;
			}
			
			return count;
		}
		
		return 0;
	}
	
	public static String toHexString(long value, int length) {
		if(length < 1) throw new IllegalArgumentException("The minimum length of the returned string cannot be less than one.");
		return String.format("%0" + length + "x", value);
	}
	
	// NOTE: Why is this printHexString method not using a byte array?
	public static String printHexString(CharSequence separator, int[] array) {
		if(array == null || array.length == 0) return "";
		StringBuilder sb = new StringBuilder();
		for(int i : array) {
			sb.append(separator).append(String.format("%02x", i & 0xff));
		}
		
		return sb.toString().substring(separator.length()).trim();
	}
	
	// NOTE: Why is this printHexString method not using a byte array?
	public static String printHexString(CharSequence separator, byte[] array) {
		if(array == null || array.length == 0) return "";
		StringBuilder sb = new StringBuilder();
		for(int i : array) {
			sb.append(separator).append(String.format("%02x", i & 0xff));
		}
		
		return sb.toString().substring(separator.length()).trim();
	}
}
