package language.utils.position;

public class SyntaxUtils {
    public static boolean syntaxIntersect(ISyntaxPosition syntaxPosition, Position pos) {
        Position start = syntaxPosition.getStartPosition();
        Position end = syntaxPosition.getEndPosition();

        return (pos.line >= start.line && pos.line <= end.line)
                && (pos.line != start.line || pos.column >= start.column)
                && (pos.line != end.line || pos.column < end.column);
    }

    public static boolean syntaxIntersect(ISyntaxPosition a, ISyntaxPosition b) {
        return syntaxIntersect(a, b.getStartPosition())
                || syntaxIntersect(a, b.getEndPosition());
    }
}
