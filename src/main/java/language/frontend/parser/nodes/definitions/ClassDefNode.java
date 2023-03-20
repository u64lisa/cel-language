package language.frontend.parser.nodes.definitions;


import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;

import java.util.ArrayList;
import java.util.List;

public class ClassDefNode extends Node {
    public final Token className;

    public final List<AttributeDeclareNode> attributes;
    public final List<MethodDeclareNode> methods;
    public final List<Token> argumentTypes;
    public final List<Token> generics;
    public final List<Node> defaults;
    public final List<Token> argumentNames;

    public final Node make;

    public final int defaultCount;

    public final Token parentToken;
    public final String argname;
    public final String kwargname;

    public ClassDefNode(Token className, List<AttributeDeclareNode> attributes, List<Token> argumentNames,
                        List<Token> argumentTypes, Node make, List<MethodDeclareNode> methods, Position endPosition,
                        List<Node> defaults, int defaultCount, Token pTK, List<Token> generics, String argname,
                        String kwargname) {
        this.className = className;
        this.defaultCount = defaultCount;
        this.generics = generics;
        this.defaults = defaults;
        this.attributes = attributes;
        this.make = make;
        this.argumentNames = argumentNames;
        this.argumentTypes = argumentTypes;
        this.methods = methods;
        this.endPosition = endPosition;
        this.startPosition = className.getStartPosition();
        this.argname = argname;
        this.kwargname = kwargname;

        parentToken = pTK;
        nodeType = NodeType.CLASS_DEFINITION;
    }

    @Override
    public Node optimize() {
        List<AttributeDeclareNode> optimizedAttributes = new ArrayList<>();
        for (AttributeDeclareNode attr : attributes) {
            optimizedAttributes.add((AttributeDeclareNode) attr.optimize());
        }
        Node optimizedMake = make.optimize();
        List<MethodDeclareNode> optimizedMethods = new ArrayList<>();
        for (MethodDeclareNode method : methods) {
            optimizedMethods.add((MethodDeclareNode) method.optimize());
        }
        List<Node> optimizedDefaults = new ArrayList<>();
        for (Node default_ : defaults) {
            optimizedDefaults.add(default_.optimize());
        }
        return new ClassDefNode(className, optimizedAttributes, argumentNames, argumentTypes,
                optimizedMake, optimizedMethods, getEndPosition(), optimizedDefaults, defaults.size(),
                parentToken, generics, argname, kwargname);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>(attributes);
        children.add(make);
        children.addAll(methods);
        return children;
    }

    @Override
    public String visualize() {
        return "class " + className.getValue();
    }
}
