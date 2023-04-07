package language.backend.compiler.bytecode;

import language.backend.compiler.bytecode.types.GenericType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.objects.*;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.definitions.*;
import language.frontend.parser.nodes.expressions.*;
import language.frontend.parser.nodes.operations.BinOpNode;
import language.frontend.parser.nodes.operations.UnaryOpNode;
import language.frontend.parser.nodes.values.EnumNode;
import language.frontend.parser.nodes.values.NumberNode;
import language.frontend.parser.nodes.values.RefNode;
import language.frontend.parser.nodes.variables.AttributeAccessNode;
import language.frontend.parser.nodes.variables.VarAccessNode;
import language.frontend.parser.units.EnumChild;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;

import java.util.*;

public class TypeLookup {

    final Compiler compiler;
    final Map<String, Type> types;

    public TypeLookup(Compiler compiler) {

        this.compiler = compiler;
        this.types = new HashMap<>();
        // Builtin types
        types.put("i32", Types.INT);
        types.put("f32", Types.FLOAT);
        types.put("i128", Types.LONG);
        types.put("i64", Types.DOUBLE);
        types.put("i16", Types.SHORT);
        types.put("i8", Types.BYTE);

        types.put("bool", Types.BOOL);
        types.put("String", Types.STRING);
        types.put("list", Types.LIST);
        types.put("map", Types.MAP);
        types.put("void", Types.VOID);
        types.put("any", Types.ANY);
        types.put("bytearray", Types.BYTES);
        types.put("catcher", Types.RESULT);
    }

    public Type getType(String name) {
        if (types.containsKey(name)) {
            return types.get(name);
        }
        if (compiler.enclosingType instanceof ClassType type) {
            if (type.genericMap.containsKey(name)) {
                return type.genericMap.get(name);
            }
        }
        if (compiler.enclosingType instanceof EnumChildType type) {
            if (type.propertyGenericMap.containsKey(name)) {
                return type.propertyGenericMap.get(name);
            }
        }
        if (compiler.enclosing != null) {
            return compiler.enclosing.typeHandler.getType(name);
        }

        return null;
    }

    private class TypeParser {
        final List<String> data;
        int i;
        String currentToken;

        public TypeParser(List<String> data) {
            this.data = data;
            this.i = -1;
            this.advance();
        }

        private void advance() {
            i++;
            if (i >= data.size()) {
                currentToken = null;
            } else {
                currentToken = data.get(i);
            }
        }

        private void fail(String reason) {
            compiler.error("TypeParser", reason);
        }

        private void consume(String token) {
            if (currentToken == null || !currentToken.equals(token)) {
                fail("Expected '" + token + "' but got '" + currentToken + "'");
            }
            advance();
        }

        private Type subType() {
            // Basic: HEADER
            // ex. int, float, String, CoolClass
            // Reference: [TYPE]
            // ex. [int], [float], [String], [CoolClass], [CoolClass<int>]
            // Generic: HEADER ( TYPEA, TYPEB, TYPEC, ... )
            // ex. CoolClass<int, float, String>, CoolClass<CoolClass<int>>
            // Function: ReturnType < T1, T2, ... Tn >
            // ex. int<int, float>
            String header = currentToken;
            Type type = getType(header);
            advance();
            if (header.equals("(")) {
                // Grouping
                type = subType();
                if (currentToken != null && currentToken.equals(",")) {
                    List<Type> tuple = new ArrayList<>();
                    tuple.add(type);
                    while (currentToken != null && currentToken.equals(",")) {
                        consume(",");
                        tuple.add(subType());
                    }
                    type = new TupleType(tuple.toArray(new Type[0]));
                }
                consume(")");
            }
            if (header.equals("[")) {
                // Reference
                Type refType = subType();
                consume("]");
                return new ReferenceType(refType);
            }
            if (type == null) {
                fail(String.format("Unknown type '%s'", header));
            }
            if (type instanceof ClassType classType) {
                List<Type> genericTypes = new ArrayList<>();
                if ("(".equals(currentToken)) {
                    // Generic
                    do {
                        advance();
                        genericTypes.add(subType());
                    } while (",".equals(currentToken));
                    consume(")");
                }
                if (genericTypes.size() != classType.generics.length) {
                    fail(String.format("Expected %d generic types but got %d", classType.constructor.generics.length, genericTypes.size()));
                }
                type = new InstanceType(classType, genericTypes.toArray(new Type[0]));
            }

            if ("<".equals(currentToken)) {
                advance();
                // Function
                boolean varargs = false;
                boolean defaultArgs = false;
                int defaultCount = 0;
                List<Type> argTypes = new ArrayList<>();
                while (currentToken != null && !">".equals(currentToken)) {
                    if (currentToken.equals("*")) {
                        if (defaultArgs) {
                            fail("Varargs cannot mix with default arguments");
                        }
                        varargs = true;
                        advance();
                        break;
                    }
                    argTypes.add(subType());

                    if ("?".equals(currentToken)) {
                        defaultArgs = true;
                        advance();
                        defaultCount++;
                    } else if (defaultArgs) {
                        fail("Default arguments must be last");
                    }

                    if (",".equals(currentToken)) {
                        advance();
                    }
                }
                consume(">");
                type = new ClassObjectType(type, argTypes.toArray(new Type[0]), new GenericType[0], varargs, defaultCount);
            }

            return type;
        }

        public Type resolve() {
            Type type = subType();
            if (currentToken != null) {
                fail("Unexpected token '" + currentToken + "'");
            }
            return type;
        }
    }

    public Type resolve(Token type) {
        return resolve((List<String>) type.getValue());
    }

    public Type resolve(List<String> type) {
        return new TypeParser(type).resolve();
    }

    public Type resolve(Node statement) {
        switch (statement.getNodeType()) {
            case USE, NULL, PASS, BREAK, CONTINUE, BODY, DESTRUCT, DYNAMIC_ASSIGN, LET, DROP, THROW, ASSERT, COMPILER, SWITCH, PATTERN, MACRO_DEFINITION, TYPE_DEFINITION -> {
                return Types.VOID;
            }
            case CAST -> {
                return resolve(((CastNode) statement).type);
            }
            case BIN_OP -> {
                return resolve((BinOpNode) statement);
            }
            case UNARY_OPERATION -> {
                return resolve((UnaryOpNode) statement);
            }
            case DECORATOR -> {
                return resolve((DecoratorNode) statement);
            }
            case INLINE_DEFINITION -> {
                return resolve((InlineDeclareNode) statement);
            }
            case CALL -> {
                return resolve((CallNode) statement);
            }
            case RETURN -> {
                return resolve((ReturnNode) statement);
            }
            case SPREAD -> {
                return Types.SPREAD;
            }
            case NUMBER -> {
                NumberNode node = (NumberNode) statement;
                return (long) node.val == node.val ? Types.INT : Types.FLOAT;
            }
            case STRING -> {
                return Types.STRING;
            }
            case BOOLEAN -> {
                return Types.BOOL;
            }
            case LIST -> {
                return Types.LIST;
            }
            case MAP -> {
                return Types.MAP;
            }
            case BYTES -> {
                return Types.BYTES;
            }

            // This is an if statement
            case SCOPE, QUERY -> {
                return Types.ANY;
            }
            case ENUM -> {
                return resolve((EnumNode) statement);
            }
            case CLASS_DEFINITION -> {
                return resolve((ClassDefNode) statement);
            }
            case CLASS_ACCESS -> {
                ClassAccessNode node = (ClassAccessNode) statement;
                Type clazz = resolve(node.className);
                String attr = node.attributeName.getValue().toString();
                Type attrType = clazz.access(attr);
                // If they are memory similar, then it is "this"
                if (clazz == compiler.enclosingType) {
                    attrType = compiler.accessEnclosed(attr);
                }
                if (attrType == null) {
                    compiler.error("Attribute", String.format("'%s' object has no attribute '%s'", clazz, attr));
                }
                return attrType;
            }
            case ATTRIBUTE_ASSIGN -> {
                AttributeAssignNode node = (AttributeAssignNode) statement;
                Type newAttr = resolve(node.value);
                String attr = node.name.getValue().toString();
                Type oldAttr = compiler.accessEnclosed(attr);
                if (oldAttr == null) {
                    compiler.error("Attribute", String.format("'%s' object has no attribute '%s'", compiler.enclosingType, attr));
                } else if (!oldAttr.equals(newAttr) && oldAttr != Types.ANY) {
                    compiler.error("Type", String.format("Cannot assign '%s' to '%s'", newAttr, oldAttr));
                }
                return Types.VOID;
            }
            case ATTRIBUTE_ACCESS -> {
                AttributeAccessNode node = (AttributeAccessNode) statement;
                String attr = node.name.getValue().toString();
                Type type = compiler.accessEnclosed(attr);
                if (type == null) {
                    compiler.error("Attribute", String.format("'%s' object has no attribute '%s'", compiler.enclosingType, attr));
                }
                return type;
            }
            case VAR_ACCESS -> {
                return resolve((VarAccessNode) statement);
            }
            case VAR_ASSIGNMENT -> {
                return resolve((VarAssignNode) statement);
            }
            case WHILE -> {
                return resolve((WhileNode) statement);
            }
            case FOR -> {
                return resolve((ForNode) statement);
            }
            case ITERATOR -> {
                return resolve((IterNode) statement);
            }
            case REFERENCE -> {
                return resolve((RefNode) statement);
            }
            case DE_REF -> {
                return resolve((DerefNode) statement);
            }
            default -> throw new RuntimeException("Unknown statement type: " + statement.getNodeType());
        }
    }

    private Type resolve(BinOpNode statement) {
        Type left = resolve(statement.left);
        Type right = resolve(statement.right);
        Type result = left.isCompatible(statement.operation, right);

        if (result == null) {
            compiler.error("Type", String.format("Cannot apply '%s' to '%s' and '%s'", statement.operation,
                    left, right));
        }
        return result;
    }

    private Type resolve(UnaryOpNode statement) {
        Type right = resolve(statement.node);
        Type result = right.isCompatible(statement.operation);
        if (result == null) {
            compiler.error("Type", String.format("Cannot apply '%s' to '%s'", statement.operation, right));
        }
        return result;
    }

    private Type resolve(DecoratorNode node) {
        Type decorator = resolve(node.decorator);
        Type decorated = resolve(node.decorated);
        Type result = decorator.call(new Type[]{decorated}, new Type[0]);
        if (!decorated.equals(result)) {
            compiler.error("Decorator", "Decorator must return the decorated function");
        }
        return result;
    }

    private Type resolve(InlineDeclareNode node) {
        // Insert generic types into type map
        GenericType[] generics = new GenericType[node.generics.size()];
        Set<String> removeLater = new HashSet<>();
        for (int index = 0; index < node.generics.size(); index++) {
            String generic = node.generics.get(index).getValue().toString();
            generics[index] = new GenericType(generic);

            if (!types.containsKey(generic)) {
                removeLater.add(generic);
                types.put(generic, generics[index]);
            }
        }

        Type returnType = resolve(node.returnType);
        Type[] argTypes = new Type[node.argumentTypes.size()];
        for (int index = 0; index < node.argumentTypes.size(); index++) {
            Token argTypeTok = node.argumentTypes.get(index);
            argTypes[index] = resolve(argTypeTok);
        }

        // Remove newly introduced generic types from type map
        for (String generic : removeLater)
            types.remove(generic);

        return new ClassObjectType(returnType, argTypes, generics, node.argname != null, node.defaultCount);
    }

    private Type resolve(CallNode node) {
        Type func = resolve(node.nodeToCall);
        Type[] argTypes = new Type[node.argNodes.size()];
        for (int index = 0; index < node.argNodes.size(); index++) {
            argTypes[index] = resolve(node.argNodes.get(index));
        }
        Type[] generics = new Type[node.generics.size()];
        for (int index = 0; index < node.generics.size(); index++) {
            generics[index] = resolve(node.generics.get(index));
        }
        Type result = func.call(argTypes, generics);
        if (result == null) {
            compiler.error("Call", "Cannot call function with the given arguments");
        }
        return result;
    }

    private Type resolve(ReturnNode node) {
        Type result;
        if (node.nodeToReturn == null) {
            result = Types.VOID;
        } else {
            result = resolve(node.nodeToReturn);
        }

        if (!compiler.classObjectType.returnType.equals(result)) {
            compiler.error("Return", "Return type must be the same as the function's return type");
        }
        return Types.VOID;
    }

    private Type resolve(EnumNode node) {
        EnumChildType[] children = new EnumChildType[node.children.size()];
        for (int index = 0; index < node.children.size(); index++) {
            EnumChild child = node.children.get(index);
            String[] properties = child.params().toArray(new String[0]);
            Type[] propertyTypes = new Type[child.types().size()];
            for (int typeIndex = 0; typeIndex < propertyTypes.length; typeIndex++) {
                Type type = resolve(child.types().get(typeIndex));
                propertyTypes[typeIndex] = type;
            }
            GenericType[] generics = new GenericType[child.generics().size()];
            for (int genericIndex = 0; genericIndex < generics.length; genericIndex++) {
                generics[genericIndex] = new GenericType(child.generics().get(genericIndex));
            }
            children[index] = new EnumChildType(child.token().getValue().toString(), propertyTypes, generics, properties);
        }
        Type type = new EnumType(node.tok.getValue().toString(), children);
        types.put(node.tok.getValue().toString(), type);
        return type;
    }

    private Type resolve(ClassDefNode node) {
        String name = node.className.getValue().toString();
        ClassType parent = null;
        if (node.parentToken != null) {
            Type unknownType = getType(node.parentToken.getValue().toString());
            if (unknownType == null) {
                compiler.error("Class", "Parent class must be defined before it is used");
            }
            if (!(unknownType instanceof ClassType)) {
                compiler.error("Class", "Parent must be a class");
            }
            assert unknownType instanceof ClassType;
            parent = (ClassType) unknownType;
        }

        Position constructorStart = node.make.getStartPosition();
        Position constructorEnd = node.make.getEndPosition();
        InlineDeclareNode constructorNode = new InlineDeclareNode(
                new Token(TokenType.IDENTIFIER, "<make>", constructorStart, constructorEnd),
                node.argumentNames,
                node.argumentTypes,
                node.make,
                false,
                false,
                Collections.singletonList("void"),
                node.defaults,
                node.defaultCount,
                node.generics,
                node.argname,
                node.kwargname
        );

        GenericType[] generics = new GenericType[constructorNode.generics.size()];
        Set<String> removeLater = new HashSet<>();
        for (int index = 0; index < constructorNode.generics.size(); index++) {
            generics[index] = new GenericType(constructorNode.generics.get(index).getValue().toString());

            if (!types.containsKey(generics[index].name)) {
                removeLater.add(generics[index].name);
                types.put(generics[index].name, generics[index]);
            }
        }

        ClassType type = new ClassType(name, parent, null, new HashMap<>(), new HashSet<>(), new HashMap<>(), new HashMap<>(), generics);
        types.put(name, type);

        ClassObjectType constructor = (ClassObjectType) resolve(constructorNode);
        Map<String, Type> fields = new HashMap<>();
        Set<String> privates = new HashSet<>();
        Map<String, Type> staticFields = new HashMap<>();
        Map<String, Type> operators = new HashMap<>();

        for (AttributeDeclareNode attr : node.attributes) {
            if (attr.isprivate) {
                privates.add(attr.name);
            }
            Type attrType = resolve(attr.type);
            if (attr.isstatic) {
                staticFields.put(attr.name, attrType);
            } else {
                fields.put(attr.name, attrType);
            }
        }
        for (MethodDeclareNode method : node.methods) {
            String funcName = method.name.getValue().toString();
            if (method.priv) {
                privates.add(funcName);
            }
            Type methType = resolve(method.asFuncDef());
            if (method.stat) {
                staticFields.put(funcName, methType);
            } else if (method.bin) {
                operators.put(funcName, methType);
            } else {
                fields.put(funcName, methType);
            }
        }

        type.fields.putAll(fields);
        type.fields.putAll(operators);
        type.staticFields.putAll(staticFields);
        type.operators.putAll(operators);
        type.privates.addAll(privates);
        type.constructor = constructor;

        for (String generic : removeLater)
            types.remove(generic);

        return type;
    }

    private Type resolve(VarAccessNode node) {
        return compiler.variableType(node.name.getValue().toString());
    }

    private Type resolve(VarAssignNode node) {
        if (node.defining) {
            return Types.VOID;
        } else {
            return resolve(node.value);
        }
    }

    private Type resolve(WhileNode node) {
        return node.retnull ? Types.VOID : Types.LIST;
    }

    private Type resolve(ForNode node) {
        return node.retnull ? Types.VOID : Types.LIST;
    }

    private Type resolve(IterNode node) {
        return node.retnull ? Types.VOID : Types.LIST;
    }

    private Type resolve(RefNode node) {
        return new ReferenceType(resolve(node.inner));
    }

    private Type resolve(DerefNode node) {
        Type dereferencing = resolve(node.ref);
        if (!(dereferencing instanceof ReferenceType)) {
            compiler.error("Deref", "Cannot dereference non-reference type");
        }
        assert dereferencing instanceof ReferenceType;
        return ((ReferenceType) dereferencing).ref;
    }
}
