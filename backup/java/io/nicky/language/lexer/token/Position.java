package language.frontend.lexer.token;

@SuppressWarnings("unused")
public class Position {
    public static final Position EMPTY = new Position(0, 0, 0, "", "");

    protected int index;
    protected int line;
    protected int column;

    protected int currentColumn = 0;
    protected int currentLine;

    protected final String file;
    protected final String source;

    public Position(int index, int line, int column, String file, String source) {
        this.index = index;
        this.line = line;
        this.column = column;
        this.file = file;
        this.source = source;
    }



    public Position setCurrent(int currentColumn, int currentLine) {
        this.currentColumn = currentColumn;
        this.currentLine = currentLine;
        return this;
    }

    public Position setSourcePosition(int column, int line) {
        this.column = column;
        this.line = line;

        return this;
    }

    public void advance(char current) {
        index++;
        column++;

        if (current != '\t') {
            currentColumn++;
            currentLine++;
        }

        if (current == '\n') {
            line++;
            column = 0;
            currentColumn = 0;
        }

    }

    public Position advance(int column) {
        Position position = this.copy();

        position.setColumn(this.column + column);
        return position;
    }

    public Position advance() {
        index++;
        column++;
        return this;
    }

    public Position copy() {
        return new Position(index, line, column, file, source).setCurrent(currentColumn, currentLine);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getFile() {
        return file;
    }

    public String getSource() {
        return source;
    }

    public int getCurrentColumn() {
        return currentColumn;
    }

    public int getcurrentLine() {
        return currentLine;
    }

    @Override
    public String toString() {
        return "Position{" +
                "column=" + currentColumn +
                ", line=" + currentLine +
                '}';
    }
}
