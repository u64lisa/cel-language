package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InlineDeclareNode extends Node {
    public final Token name;
    public final List<Token> argumentNames;
    public final Node body;
    public final boolean autoreturn;
    public final boolean async;
    public final List<Token> argumentTypes;
    public final List<Token> generics;
    public final List<String> returnType;
    public final List<Node> defaults;
    public final int defaultCount;
    public boolean catcher = false;
    public final String argname;
    public final String kwargname;

    public InlineDeclareNode(Token name, List<Token> argumentNames, List<Token> argumentTypes, Node body,
                             boolean autoreturn, boolean async, List<String> returnType, List<Node> defaults, int defaultCount,
                             List<Token> generics, String argname, String kwargname) {
        this.name = name;
        this.argname = argname;
        this.kwargname = kwargname;
        this.generics = generics;
        this.async = async;
        this.argumentNames = argumentNames;
        this.argumentTypes = argumentTypes;
        this.body = body;
        this.autoreturn = autoreturn;
        this.returnType = returnType;
        this.defaults = defaults;
        this.defaultCount = defaultCount;

        startPosition = name != null ? name.getStartPosition() : (argumentNames != null && argumentNames.size() > 0 ?
                argumentNames.get(0).getStartPosition() : body.getStartPosition());

        endPosition = body.getEndPosition();
        nodeType = NodeType.INLINE_DEFINITION;
    }

    public InlineDeclareNode setCatcher(boolean c) {
        this.catcher = c;
        return this;
    }

    @Override
    public Node optimize() {
        Node body = this.body.optimize();
        List<Node> optimizedDefaults = new ArrayList<>();
        for (Node n : defaults) {
            optimizedDefaults.add(n.optimize());
        }
        return new InlineDeclareNode(name, argumentNames, argumentTypes, body, autoreturn, async, returnType,
                optimizedDefaults, defaultCount, generics, argname, kwargname).setCatcher(catcher);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(body));
    }

    @Override
    public String visualize() {
        return "inline" + (
                name != null ?
                " " + name.getValue() : ""
        ) + "(" + argumentNames.stream().map(x -> x.getValue().toString()).collect(Collectors.joining(", ")) + ")";
    }

    public Token getName() {
        return name;
    }

    public List<Token> getArgumentNames() {
        return argumentNames;
    }

    public Node getBody() {
        return body;
    }

    public boolean isAutoreturn() {
        return autoreturn;
    }

    public boolean isAsync() {
        return async;
    }

    public List<Token> getArgumentTypes() {
        return argumentTypes;
    }

    public List<Token> getGenerics() {
        return generics;
    }

    public List<String> getReturnType() {
        return returnType;
    }

    public List<Node> getDefaults() {
        return defaults;
    }

    public int getDefaultCount() {
        return defaultCount;
    }

    public boolean isCatcher() {
        return catcher;
    }

    public String getArgname() {
        return argname;
    }

    public String getKwargname() {
        return kwargname;
    }
}
