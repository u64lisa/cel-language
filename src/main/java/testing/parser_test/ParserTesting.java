package testing.parser_test;

import dtool.logger.PositionedError;
import dtool.logger.errors.ExceptionHighlighter;
import language.frontend.lexer.Lexer;
import language.frontend.parser.Parser;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.results.ParseResult;

public class ParserTesting {
    private static int test;


    public static void main(String[] args) {
        checkOK("1 + 1");
    }

    public static void checkOK(final String program) {
        test++;

        if (program == "" || program == null) {
            System.err.println("Failed at test: " + test);
            System.exit(-1);
        }

        Lexer lexer = new Lexer("testing", program);
        Parser parser = Parser.getParser(lexer.lex());

        ParseResult<Node> parseResult = parser.parse();

        if (parseResult.getLanguageError() != null) {
            ExceptionHighlighter exceptionHighlighter = new ExceptionHighlighter();
            PositionedError positionedError = new PositionedError(parseResult.getLanguageError(), "testing", program);
            String value = exceptionHighlighter.createFullError(positionedError);
            System.err.println(value);
            System.err.println("Failed at test: " + test);
            System.exit(-1);
        }
        System.out.println("Passed test: " + test + "!");
    }

}
