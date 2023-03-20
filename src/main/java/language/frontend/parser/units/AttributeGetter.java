package language.frontend.parser.units;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.results.ParseResult;
import language.frontend.lexer.token.Token;

public interface AttributeGetter {
    ParseResult<Node> run(Token tok, boolean b1, boolean b2);
}
