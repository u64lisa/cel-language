package language.frontend.parser;

import language.frontend.parser.nodes.Node;
import language.frontend.parser.results.ParseResult;

public interface NodeCallback {
    ParseResult<Node> call();

}
