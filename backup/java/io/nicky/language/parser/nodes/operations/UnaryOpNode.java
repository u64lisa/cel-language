package language.frontend.parser.nodes.operations;

import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.TokenType;

import language.vm.VirtualMachine;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.values.BooleanNode;
import language.frontend.parser.nodes.values.NumberNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnaryOpNode extends Node {

    public final TokenType operation;
    public final Node node;

    public UnaryOpNode(TokenType operation, Node node) {
        this.node = node;
        this.operation = operation;

        constant = node.isConstant();

        startPosition = node.getStartPosition().copy();
        endPosition = node.getEndPosition().copy();
        nodeType = NodeType.UNARY_OPERATION;
    }

    public String toString() {
        return String.format("(%s, %s)", operation, node);
    }

    /*
     * All unary operations:
     * - $ => From Bytes
     * - ~ => Bitwise Complement
     * - - => Negative
     * - ! => Logical Not
     * - -- => Decrement
     * - ++ => Increment
     * - + => Stay the same
     */

    @Override
    public Node optimize() {
        if (node.isConstant() && operation != TokenType.TILDE) {
            Node node = this.node.optimize();

            return switch (operation) {
                case MINUS -> new NumberNode(-node.asNumber(), node.getStartPosition(), node.getEndPosition());
                case TILDE -> new NumberNode(VirtualMachine.bitOp(
                        node.asNumber(),
                        0,
                        (a, b) -> ~a
                ), node.getStartPosition(), node.getEndPosition());
                case BANG -> new BooleanNode(!node.asBoolean(), node.getStartPosition(), node.getEndPosition());
                case MINUS_MINUS, PLUS_PLUS ->
                        new NumberNode(node.asNumber() + (operation == TokenType.PLUS_PLUS ? 1.0 : -1.0), node.getStartPosition(), node.getEndPosition());
                default -> node;
            };
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(node));
    }

    @Override
    public String visualize() {
        switch (operation) {
            case MINUS:
                return "-";
            case PLUS:
                return "+";
            case DOLLAR:
                return "$";
            case TILDE:
                return "~";
            case BANG:
                return "!";
            case MINUS_MINUS:
                return "--";
            case PLUS_PLUS:
                return "++";
            default:
                return "";
        }
    }
}
