package language.frontend.parser.nodes.expressions;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import language.frontend.parser.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class ImportNode extends Node {
    public final Token fileName;
    public final Token customName;

    public ImportNode(Token name) {
        this.fileName = name;
        this.customName = null;

        startPosition = name.getStartPosition().copy(); endPosition = name.getEndPosition().copy();
        nodeType = NodeType.IMPORT;
    }

    public ImportNode(Token fileName, Token customName) {
        this.fileName = fileName;
        this.customName = customName;

        startPosition = fileName.getStartPosition().copy();
        endPosition = customName.getEndPosition().copy();

        nodeType = NodeType.IMPORT;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        return "import " + fileName.getValue();
    }
}
