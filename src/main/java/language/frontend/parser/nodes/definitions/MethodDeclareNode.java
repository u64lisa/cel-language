package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodDeclareNode extends Node {
    public final Token name;
    public final List<Token> argumentNames;
    public final Node body;
    public final boolean autoreturn;
    public final boolean async;
    public final boolean bin;
    public final List<Token> argumentTypes;
    public final List<Token> generics;
    public final List<String> returnType;
    public final List<Node> defaults;
    public final int defaultCount;
    public boolean catcher = false;
    public final boolean stat;
    public final boolean priv;
    public final String argname;
    public final String kwargname;

    public MethodDeclareNode(Token name, List<Token> argumentNames, List<Token> argumentTypes, Node body,
                             boolean autoreturn, boolean bin, boolean async, List<String> returnType, List<Node> defaults,
                             int defaultCount, List<Token> generics, boolean stat, boolean priv, String argname,
                             String kwargname) {
        this.name = name;
        this.stat = stat;
        this.priv = priv;
        this.generics = generics;
        this.async = async;
        this.argumentNames = argumentNames;
        this.argumentTypes = argumentTypes;
        this.body = body;
        this.autoreturn = autoreturn;
        this.bin = bin;
        this.returnType = returnType;
        this.defaults = defaults;
        this.defaultCount = defaultCount;
        this.argname = argname;
        this.kwargname = kwargname;

        startPosition = name.getStartPosition();
        endPosition = body.getEndPosition();
        nodeType = NodeType.METHOD_DEFINITION;
    }

    public MethodDeclareNode setCatcher(boolean c) {
        this.catcher = c;
        return this;
    }

    public InlineDeclareNode asFuncDef() {
        return new InlineDeclareNode(name, argumentNames, argumentTypes, body, autoreturn, async,
                returnType, defaults, defaultCount, generics, argname, kwargname).setCatcher(catcher);
    }

    @Override
    public Node optimize() {
        Node body = this.body.optimize();
        List<Node> optDefaults = new ArrayList<>();
        for (Node n : defaults) {
            optDefaults.add(n.optimize());
        }
        return new MethodDeclareNode(name, argumentNames, argumentTypes, body, autoreturn, bin, async,
                returnType, optDefaults, defaultCount, generics, stat, priv, argname, kwargname)
                .setCatcher(catcher);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(body));
    }

    @Override
    public String visualize() {
        String stc = stat ? "static " : "";
        String prv = priv ? "private " : "public ";
        return prv + stc + "method " + name.getValue() + "(" + argumentNames.stream().map(x -> x.getValue().toString()).reduce("", (a, b) -> a + b + ",") + ")";
    }
}
