package dtool.logger.errors;

import language.frontend.lexer.token.Position;

import java.util.Arrays;


public record LanguageException(Type type, Position positionStart, Position positionEnd, String errorName,
                                String... details) {

    public enum Type {

        PARSER,
        COMPILER,
        VIRTUAL_MACHINE

    }

    public LanguageException(Type type, String errorName, String... details) {
        this(type, Position.EMPTY, Position.EMPTY, errorName, details);
    }

    public LanguageException(String errorName, String... details) {
        this(Type.PARSER, Position.EMPTY, Position.EMPTY, errorName, details);
    }

    public LanguageException(Type type, Position positionStart, Position positionEnd, String errorName, String... details) {
        this.type = type;

        this.positionStart = positionStart != null ? positionStart.copy() : null;
        this.positionEnd = positionEnd != null ? positionEnd.copy() : null;
        this.errorName = errorName;
        this.details = details;
    }

    public static LanguageException illegalChar(Position start, Position end, String details) {
        return new LanguageException(Type.PARSER, start, end, "Illegal Character", details);
    }

    public static LanguageException expected(Position start, Position end, String details) {
        return new LanguageException(Type.PARSER, start, end, "Expected Character", details);
    }

    public static LanguageException invalidSyntax(Position start, Position end, String details) {
        return new LanguageException(Type.PARSER, start, end, "Invalid Syntax", details);
    }

    @Override
    public String toString() {
        return "LanguageException{" +
                "type=" + type +
                ", positionStart=" + positionStart +
                ", positionEnd=" + positionEnd +
                ", errorName='" + errorName + '\'' +
                ", details=" + Arrays.toString(details) +
                '}';
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Position positionStart() {
        return positionStart;
    }

    @Override
    public Position positionEnd() {
        return positionEnd;
    }

    @Override
    public String errorName() {
        return errorName;
    }

    @Override
    public String[] details() {
        return details;
    }

}
