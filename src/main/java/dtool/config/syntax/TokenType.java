package dtool.config.syntax;

public enum TokenType {
    // Whitespace
    WHITESPACE,
    EOF,

    // Test
    FUNCTION,
    COMMA,

    // Comparisons
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    LESS_EQUAL,
    MORE_THAN,
    MORE_EQUAL,
    CAND,
    COR,

    // Brackets
    L_PAREN,
    R_PAREN,
    L_SQUARE,
    R_SQUARE,
    L_CURLY,
    R_CURLY,

    // Arithmetics
    NOT,
    NOR,
    AND,
    OR,
    XOR,
    PLUS,
    MINUS,
    MUL,
    DIV,
    MOD,
    SHIFT_RIGHT,
    SHIFT_LEFT,

    // Memory Operations
    ASSIGN,

    // Atoms
    IDENTIFIER,
    CHARACTER,
    BOOLEAN,
    STRING,
    ULONG,
    UINT,
    LONG,
    INT,
    FLOAT,
    DOUBLE,
    NULL,

    // Keywords
    IF,
    ELSE,
    FOR,
    WHILE,
    DEFAULT,
    SWITCH,
    CONTINUE,
    BREAK,
    RETURN,
    NAMESPACE,
    TYPEDEF,
    DESTRUCT,

    // Compiler annotations
    COMPILER,

    // Function Modifiers
    EXPORT,
    INLINE,
    CONSTANT,
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    ABSTRACT,

    // Delimiters
    SEMICOLON,
    QUESTION_MARK,
    VARARGS,
    COLON,

    // Classes
    NAMESPACE_OPERATOR,
    MODULE,

    // Preprocessors
    LINK,
    PREP_IF,
    PREP_IFDEF,
    PREP_ENDIF,
    PREP_ELSE,
    PREP_ELSEIF,
    PREP_USING,
    EXTERN,

    CASE,

    // Reserved
    RESERVED, STRUCT, UNION, ENUM, INTERFACE, IMPORT,
}
