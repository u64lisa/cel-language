package io.nicky.language.workspace;

import language.frontend.lexer.token.Token;
import language.frontend.parser.nodes.Node;

import java.util.List;
import java.util.function.Consumer;

public interface Workspace {

    Workspace init();

    Workspace checkFolder();

    Workspace compile();

    Workspace execute(final String[] arguments);

    Workspace displayProfiling();

    Workspace tokens(Consumer<List<Token>> tokens);

    Workspace ast(Consumer<List<Node>> ast);
}
