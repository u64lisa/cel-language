package dtool.config.syntax.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ErrorUtil {

	@Deprecated
	public static String createError(ISyntaxPos error, String message) {
		try {
			return createError(error, Files.readString(Path.of(error.getPath())), message);
		} catch (IOException e) {
			return null;
		}
	}
	
	public static String createError(ISyntaxPos error, String content, String message) {
		int errorLine = error.getEndPosition().line() + 1;
		int errorStart = error.getStartPosition().column();
		int errorEnd = error.getEndPosition().column();
		int columns = Math.max(1, errorEnd - errorStart);
		int padSize = Math.max(1, (int) Math.floor(Math.log10(errorLine)) + 1);
		
		String numPadding = " ".repeat(padSize);
		String numFormat = "%" + padSize + "d";
		String errPadding = " ".repeat(errorStart);
		
		StringBuilder sb = new StringBuilder();
		
		if (content != null) {
			List<String> lines = content.lines().toList();
			String errString = lines.get(errorLine - 1);
			
			sb.append('\n');
			sb.append("%s | %s\n".formatted(numFormat.formatted(errorLine), errString));
			sb.append("%s | %s%s\n".formatted(numPadding, errPadding, "^".repeat(columns)));
			sb.append("%s | %s%s".formatted(numPadding, errPadding, message));
		} else {
			sb.append('\n');
			sb.append("%s | %s".formatted(numFormat.formatted(errorLine), message));
		}
		
		return sb.toString();
	}
	
	public static String createFullError(ISyntaxPos error, String content, String message) {
		StringBuilder sb = new StringBuilder();
		
		Position position = error.getStartPosition();
		sb.append("(")
				.append(error.getPath())
				.append(") (line: ")
				.append(position.line() + 1)
				.append(", column: ")
				.append(position.column() + 1)
				.append("): ")
				.append(createError(error, content, message));
		
		return sb.toString();
	}
}
