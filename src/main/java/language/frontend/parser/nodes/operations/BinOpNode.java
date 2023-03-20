package language.frontend.parser.nodes.operations;

import language.frontend.parser.nodes.values.*;
import language.frontend.parser.nodes.NodeType;
import language.frontend.lexer.token.TokenType;

import language.vm.VirtualMachine;
import language.frontend.parser.nodes.Node;

import java.util.*;

public class BinOpNode extends Node {
    public final Node left;
    public final TokenType operation;
    public final Node right;

    public BinOpNode(Node left, TokenType operation, Node right) {
        this.left = left;
        this.operation = operation;
        this.right = right;

        constant = left.isConstant() && right.isConstant();

        startPosition = left.getStartPosition().copy();
        endPosition = right.getEndPosition().copy();

        nodeType = NodeType.BIN_OP;
    }

    public String toString() {
        return String.format("(%s, %s, %s)", left, operation, right);
    }

    /*
     * Optimizable operands:
     * - & => Return true if both operands are true
     * - | => Return true if either operands are true
     * - ~^ => Run a bitwise XOR on the two operands
     * - ~> => Run a signed right shift on the left operand by the right operand
     * - ~~ => Run a right shift on the left operand by the right operand
     * - <~ => Run a left shift on the left operand by the right operand
     * - <, >, ==, !=, >=, <=
     * - +, -, /, *, ^, %
     */

    @Override
    public Node optimize() {
        if (left.isConstant() && right.isConstant()) {
            Node left = this.left.optimize();
            Node right = this.right.optimize();
            Node opt = new BinOpNode(left, operation, right).setStatic(false);

            if (left.getClass() != right.getClass())
                return opt;

            switch (operation) {
                case AMPERSAND -> {
                    return new BooleanNode(left.asBoolean() && right.asBoolean(), getStartPosition(), getEndPosition());
                }
                case PIPE -> {
                    return new BooleanNode(left.asBoolean() || right.asBoolean(), getStartPosition(), getEndPosition());
                }
                case TILDE_AMPERSAND -> {
                    return new NumberNode(VirtualMachine.bitOp(
                            left.asNumber(),
                            right.asNumber(),
                            (a, b) -> a & b
                    ), getStartPosition(), getEndPosition());
                }
                case TILDE_PIPE -> {
                    return new NumberNode(VirtualMachine.bitOp(
                            left.asNumber(),
                            right.asNumber(),
                            (a, b) -> a | b
                    ), getStartPosition(), getEndPosition());
                }
                case TILDE_CARET -> {
                    return new NumberNode(VirtualMachine.bitOp(
                            left.asNumber(),
                            right.asNumber(),
                            (a, b) -> a ^ b
                    ), getStartPosition(), getEndPosition());
                }
                case LEFT_TILDE_ARROW -> {
                    return new NumberNode(VirtualMachine.bitOp(
                            left.asNumber(),
                            right.asNumber(),
                            (a, b) -> a << b
                    ), getStartPosition(), getEndPosition());
                }
                case TILDE_TILDE -> {
                    return new NumberNode(VirtualMachine.bitOp(
                            left.asNumber(),
                            right.asNumber(),
                            (a, b) -> a >> b
                    ), getStartPosition(), getEndPosition());
                }
                case RIGHT_TILDE_ARROW -> {
                    return new NumberNode(VirtualMachine.bitOp(
                            left.asNumber(),
                            right.asNumber(),
                            (a, b) -> a >>> b
                    ), getStartPosition(), getEndPosition());
                }
                case PLUS -> {
                    if (left instanceof NumberNode)
                        return new NumberNode(left.asNumber() + right.asNumber(), getStartPosition(), getEndPosition());
                    else if (left instanceof StringNode)
                        return new StringNode(left.asString() + right.asString(), getStartPosition(), getEndPosition());
                    else if (left instanceof ListNode) {
                        List<Node> list = new ArrayList<>();
                        list.addAll(left.asList());
                        list.addAll(right.asList());
                        return new ListNode(list, getStartPosition(), getEndPosition());
                    } else if (left instanceof MapNode) {
                        Map<Node, Node> map = new HashMap<>();
                        map.putAll(left.asMap());
                        map.putAll(right.asMap());
                        return new MapNode(map, getStartPosition(), getEndPosition());
                    }
                    return opt;
                }
                case MINUS -> {
                    return left instanceof NumberNode ? new NumberNode(left.asNumber() - right.asNumber(), getStartPosition(), getEndPosition()) : opt;
                }
                case STAR -> {
                    return left instanceof NumberNode ? new NumberNode(left.asNumber() * right.asNumber(), getStartPosition(), getEndPosition()) : opt;
                }
                case SLASH -> {
                    return left instanceof NumberNode ? new NumberNode(left.asNumber() / right.asNumber(), getStartPosition(), getEndPosition()) : opt;
                }
                case CARET -> {
                    return left instanceof NumberNode ? new NumberNode(Math.pow(left.asNumber(), right.asNumber()), getStartPosition(), getEndPosition()) : opt;
                }
                case PERCENT -> {
                    return left instanceof NumberNode ? new NumberNode(left.asNumber() % right.asNumber(), getStartPosition(), getEndPosition()) : opt;
                }
                case EQUAL_EQUAL -> {
                    return new BooleanNode(left.equals(right), getStartPosition(), getEndPosition());
                }
                case BANG_EQUAL -> {
                    return new BooleanNode(!left.equals(right), getStartPosition(), getEndPosition());
                }
                case LEFT_ANGLE -> {
                    return new BooleanNode(left.asNumber() < right.asNumber(), getStartPosition(), getEndPosition());
                }
                case LESS_EQUALS -> {
                    return new BooleanNode(left.asNumber() <= right.asNumber(), getStartPosition(), getEndPosition());
                }
                case GREATER_EQUALS -> {
                    return new BooleanNode(left.asNumber() >= right.asNumber(), getStartPosition(), getEndPosition());
                }
                case RIGHT_ANGLE -> {
                    return new BooleanNode(left.asNumber() > right.asNumber(), getStartPosition(), getEndPosition());
                }
                default -> {
                    return opt;
                }
            }
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Arrays.asList(left, right));
    }

    @Override
    public String visualize() {
        return switch (operation) {
            case AMPERSAND -> "&&";
            case PIPE -> "||";
            case FAT_ARROW -> "=>";
            case COLON -> ":";
            case TILDE_AMPERSAND -> "&";
            case TILDE_PIPE -> "|";
            case TILDE_CARET -> "^";
            case LEFT_TILDE_ARROW -> "<<";
            case TILDE_TILDE -> ">>";
            case RIGHT_TILDE_ARROW -> ">>>";
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case CARET -> "pow";
            case PERCENT -> "%";
            case EQUAL_EQUAL -> "==";
            case BANG_EQUAL -> "!=";
            case LEFT_ANGLE -> "<";
            case LESS_EQUALS -> "<=";
            case GREATER_EQUALS -> ">=";
            case RIGHT_ANGLE -> ">";
            default -> "";
        };
    }
}
