package dtool.config.syntax.utils;

import java.io.File;

public interface ISyntaxPos {
	Position getStartPosition();
	
	Position getEndPosition();
	
	String getPath();
	
	static ISyntaxPos empty(String path) {
		return new ImmutableSyntaxImpl(path, new Position(0, 0), new Position(0, 0));
	}
	
	static ISyntaxPos of(String path, Position start, Position end) {
		return new ImmutableSyntaxImpl(path, start, end);
	}
	
	static ISyntaxPos of(File file, Position start, Position end) {
		return new ImmutableSyntaxImpl(file.getAbsolutePath(), start, end);
	}
}
