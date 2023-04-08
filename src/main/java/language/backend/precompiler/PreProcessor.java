package language.backend.precompiler;

import language.frontend.parser.nodes.Node;

import java.util.List;

public interface PreProcessor {

    List<Node> transform(final List<Node> ast);

}
