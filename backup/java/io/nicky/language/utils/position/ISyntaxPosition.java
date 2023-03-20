package language.utils.position;

import java.io.File;

public interface ISyntaxPosition {
	Position getStartPosition();
	
	Position getEndPosition();
	
	static ISyntaxPosition empty() {
		return empty(null);
	}
	
	static ISyntaxPosition empty(File file) {
		return new ImmutableSyntaxImpl(new Position(file, 0, 0, 0), new Position(file, 0, 0, 0));
	}
	
	static ISyntaxPosition of(Position start, Position end) {
		return new ImmutableSyntaxImpl(start, end);
	}
	
	static ISyntaxPosition of(ISyntaxPosition start, ISyntaxPosition end) {
		return new ImmutableSyntaxImpl(start.getStartPosition(), end.getEndPosition());
	}
}
