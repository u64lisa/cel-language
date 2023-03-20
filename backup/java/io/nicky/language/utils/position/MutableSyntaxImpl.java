package language.utils.position;

public class MutableSyntaxImpl implements ISyntaxPosition {
	public Position start;
	public Position end;
	
	public MutableSyntaxImpl(Position start, Position end) {
		this.start = start;
		this.end = end;
	}

	public MutableSyntaxImpl(language.frontend.lexer.token.Position start,
                             language.frontend.lexer.token.Position end) {
		this.start = new Position(start.getLine(), start.getColumn(), 0);
		this.end = new Position(end.getLine(), end.getColumn(), 0);
	}
	
	@Override
	public Position getStartPosition() {
		return start;
	}
	
	@Override
	public Position getEndPosition() {
		return end;
	}
}
