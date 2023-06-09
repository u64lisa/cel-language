package language.backend.compiler.bytecode;

import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import dtool.logger.errors.LanguageException;
import language.backend.compiler.bytecode.headers.HeadCode;
import language.backend.compiler.bytecode.types.GenericType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.objects.*;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.backend.compiler.bytecode.values.enums.LanguageEnum;
import language.backend.compiler.bytecode.values.enums.LanguageEnumChild;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.definitions.*;
import language.frontend.parser.nodes.expressions.*;
import language.frontend.parser.nodes.extra.CompilerNode;
import language.frontend.parser.nodes.extra.MacroDefinitionNode;
import language.frontend.parser.nodes.operations.BinOpNode;
import language.frontend.parser.nodes.operations.UnaryOpNode;
import language.frontend.parser.nodes.values.*;
import language.frontend.parser.nodes.variables.AttributeAccessNode;
import language.frontend.parser.nodes.variables.TypeDefinitionNode;
import language.frontend.parser.nodes.variables.VarAccessNode;
import language.frontend.parser.units.EnumChild;
import language.utils.WrappedCast;
import language.vm.VirtualMachine;
import language.vm.library.LibraryClassLoader;
import language.vm.library.NativeContext;

import java.util.*;

public class Compiler {

    public static final Logger SYSTEM_LOGGER = ImplLogger.getInstance();

    static {
        LibraryClassLoader.LIBRARY_LOADER.indexLibraryClasses();
    }

    static class LocalToken {
        String name;

        public LocalToken(String name) {
            this.name = name;
        }
    }

    static class Local {
        final LocalToken name;
        final Type type;
        int depth;

        Local(LocalToken name, Type type, int depth) {
            this.name = name;
            this.type = type;
            this.depth = depth;
        }

    }

    static class UpValue {
        boolean isLocal;
        boolean isGlobal;

        String globalName;
        int index;
        final Type type;

        public UpValue(int index, boolean isLocal, Type type) {
            this.index = index;
            this.isLocal = isLocal;
            this.isGlobal = false;
            this.type = type;
        }

        public UpValue(String globalName, Type type) {
            this.isGlobal = true;
            this.globalName = globalName;
            this.type = type;
        }
    }

    final Compiler enclosing;

    private boolean inPattern = false;
    private Type patternType = Types.VOID;

    private final Local[] locals;
    private final Local[] generics;
    private int localCount;
    private int scopeDepth;

    private final ByteCode byteCode;

    private final Stack<Integer> continueTo;
    private final Stack<List<Integer>> breaks;

    private final Map<String, Type> globals;

    private final UpValue[] upValues;

    private final Map<String, Node> macros;
    private final Map<String, Type> macroTypes;

    protected ClassObjectType classObjectType;

    Type enclosingType;
    private boolean staticContext;

    final TypeLookup typeHandler;


    public Compiler(FunctionType type, String source, ClassObjectType classObjectType) {
        this(null, type, source, classObjectType);
        NativeContext.init(null);
        globals.putAll(NativeContext.GLOBAL_TYPES);
        globals.putAll(LibraryClassLoader.LIBRARY_TYPES);
    }

    public Compiler(Compiler enclosing, FunctionType type, String source, ClassObjectType classObjectType) {
        this.byteCode = new ByteCode(source);

        this.classObjectType = classObjectType;

        if (enclosing != null) {
            this.enclosingType = enclosing.enclosingType;
        } else {
            this.enclosingType = Types.VOID;
        }

        this.locals = new Local[VirtualMachine.MAX_STACK_SIZE];
        this.generics = new Local[VirtualMachine.MAX_STACK_SIZE];
        this.globals = new HashMap<>();

        this.upValues = new UpValue[256];

        this.localCount = 0;
        this.scopeDepth = 0;

        locals[localCount++] = new Local(new LocalToken(type == FunctionType.METHOD ||
                type == FunctionType.CONSTRUCTOR ? "this" : ""), this.enclosingType, 0);

        this.enclosing = enclosing;

        this.continueTo = new Stack<>();
        this.breaks = new Stack<>();

        this.macros = new HashMap<>();
        this.macroTypes = new HashMap<>();

        this.typeHandler = new TypeLookup(this);
    }

    public void beginScope() {
        this.scopeDepth++;
    }

    public void endScope() {
        deStackPop(locals);
        deStackPop(generics);
        scopeDepth--;
    }

    public Chunk chunk() {
        return this.byteCode.chunk;
    }

    void error(String type, String message) {
        LanguageException languageException = new LanguageException(
                LanguageException.Type.COMPILER,
                type + " Error", message
        );

        SYSTEM_LOGGER.fail("", "", languageException);
    }

    int deStack(Local[] locals) {
        int offs = 0;
        int count = 0;
        int localCount = this.localCount;
        while (localCount - offs > 0) {
            Local curr = locals[localCount - 1 - offs];
            if (curr == null) {
                offs++;
                continue;
            }
            if (curr.depth != scopeDepth) {
                break;
            }
            count++;
            localCount--;
        }
        return count;
    }

    void deStackPop(Local[] locals) {
        int count = deStack(locals);
        for (int i = 0; i < count; i++) {
            emit(ByteCodeOpCode.Pop);
            localCount--;
        }
    }

    int resolve(String name, Local[] locals) {

        for (int i = 0; i < localCount; i++) {
            Local local = locals[localCount - 1 - i];

            if (local == null)
                continue;

            if (local.name.name.equals(name))
                return localCount - 1 - i;
        }
        return -1;
    }

    Type resolveType(String name, Local[] locals) {
        for (int i = 0; i < localCount; i++) {
            Local local = locals[localCount - 1 - i];
            if (local == null) continue;
            if (local.name.name.equals(name)) return locals[localCount - 1 - i].type;
        }
        return null;
    }

    int resolveLocal(String name) {
        return resolve(name, locals);
    }

    Type resolveLocalType(String name) {
        return resolveType(name, locals);
    }

    int addUpValue(int index, boolean isLocal, Type type) {
        int upValueCount = byteCode.upvalueCount;

        for (int i = 0; i < upValueCount; i++) {
            UpValue upvalue = upValues[i];
            if (upvalue.index == index && upvalue.isLocal == isLocal) {
                return i;
            }
        }

        upValues[upValueCount] = new UpValue(index, isLocal, type);
        return byteCode.upvalueCount++;
    }

    Type resolveUpValueType(String name) {
        if (enclosing == null) return null;

        Type local = enclosing.resolveLocalType(name);
        if (local != null) {
            return local;
        }

        Type upValue = enclosing.resolveUpValueType(name);
        if (upValue != null) {
            return upValue;
        }

        return hasGlobal(name) ? getGlobal(name) : null;
    }

    int addUpValue(String name) {
        int upValueCount = byteCode.upvalueCount;

        for (int i = 0; i < upValueCount; i++) {
            UpValue upvalue = upValues[i];
            if (Objects.equals(upvalue.globalName, name) && upvalue.isGlobal) {
                return i;
            }
        }

        upValues[upValueCount] = new UpValue(name, getGlobal(name));
        return byteCode.upvalueCount++;
    }

    boolean hasGlobal(String name) {
        return globals.containsKey(name) || enclosing != null && enclosing.hasGlobal(name); // todo this breaks topdown shit blyat
    }

    Type getGlobal(String name) {
        return globals.getOrDefault(name, enclosing != null ? enclosing.getGlobal(name) : null);
    }

    int resolveUpValue(String name) {
        if (enclosing == null) return -1;

        int local = enclosing.resolveLocal(name);

        if (local != -1) {
            return addUpValue(local, true, enclosing.resolveLocalType(name));
        }

        int upValue = enclosing.resolveUpValue(name);
        if (upValue != -1) {
            return addUpValue(upValue, false, enclosing.resolveUpValueType(name));
        }

        return hasGlobal(name) ? addUpValue(name) : -1;
    }

    void patchBreaks() {
        for (int i : breaks.pop()) {
            patchJump(i);
        }
    }

    void emit(int b) {
        chunk().write(b);
    }

    void emit(int[] bs) {
        for (int b : bs)
            chunk().write(b);
    }

    void emit(int op, int b) {
        chunk().write(op);
        chunk().write(b);
    }

    int emitJump(int op) {
        emit(op);
        emit(0xff);
        return chunk().code.size() - 1;
    }

    void emitLoop(int loopStart) {
        emit(ByteCodeOpCode.Loop);

        int offset = chunk().code.size() - loopStart + 1;

        emit(offset);
    }

    void patchJump(int offset) {
        int jump = chunk().code.size() - offset - 1;
        chunk().code.set(offset, jump);
    }

    Type accessEnclosed(String name) {
        return staticContext ? enclosingType.access(name) : enclosingType.accessInternal(name);
    }

    public ByteCode compileBlock(List<Node> statements) {
        statements.removeIf(node -> node.getNodeType() == NodeType.PACKAGE);
        statements.removeIf(node -> node.getNodeType() == NodeType.IMPORT);

        for (TypeDefinitionNode node : statements
                .stream()
               .filter(node -> node.getNodeType().equals(NodeType.TYPE_DEFINITION))
                .map(node -> (TypeDefinitionNode) node)
                .toList()) {
            String typeName = node.getType().asString();
            String name = node.getName().asString();

            Type type = typeHandler.types.getOrDefault(typeName, null);
            if (type == null) {
                this.error("Typedef", "invalid type for '" + typeName + "'");
                return null;
            }

            typeHandler.types.put(name, type);
        }

        statements.removeIf(node -> node.getNodeType() == NodeType.TYPE_DEFINITION);

        for (Node statement : statements) {
            compile(statement);
            emit(ByteCodeOpCode.Pop);
        }

        emit(ByteCodeOpCode.Return);

        return endCompiler();
    }

    public ByteCode endCompiler() {
        byteCode.chunk.compile();

        return byteCode;
    }

    Type compile(Node statement) {
        switch (statement.getNodeType()) {
            case CAST -> compile(((CastNode) statement).expr);
            case BIN_OP -> compile((BinOpNode) statement);
            case UNARY_OPERATION -> compile((UnaryOpNode) statement);
            case USE -> compile((UseNode) statement);
            case DESTRUCT -> compile((DestructNode) statement);
            case DECORATOR -> compile((DecoratorNode) statement);
            case INLINE_DEFINITION -> compile((InlineDeclareNode) statement);
            case CALL -> compile((CallNode) statement);
            case RETURN -> compile((ReturnNode) statement);
            case SPREAD -> compile((SpreadNode) statement);
            case NUMBER -> {
                NumberNode node = (NumberNode) statement;
                compileNumber(node.val);
            }
            case STRING -> {
                StringNode node = (StringNode) statement;
                compileString(node.val);
            }
            case BOOLEAN -> {
                BooleanNode node = (BooleanNode) statement;
                compileBoolean(node.val);
            }
            case LIST -> compile((ListNode) statement);
            case MAP -> compile((MapNode) statement);
            case NULL, PASS -> compileNull();
            case BYTES -> compile((BytesNode) statement);
            case BODY -> {
                BodyNode node = (BodyNode) statement;
                for (Node stmt : node.statements) {
                    compile(stmt);
                    emit(ByteCodeOpCode.Pop);
                }
                compileNull();
            }
            case SCOPE -> compile((ScopeNode) statement);
            case ENUM -> {
                return compile((EnumNode) statement);
            }
            case CLASS_DEFINITION -> {
                return compile((ClassDefNode) statement);
            }
            case CLASS_ACCESS -> {
                ClassAccessNode node = (ClassAccessNode) statement;
                compile(node.className);
                String attr = node.attributeName.getValue().toString();
                int constant = chunk().addConstant(new Value(attr));
                emit(ByteCodeOpCode.Access, constant);
            }
            case ATTRIBUTE_ASSIGN -> {
                AttributeAssignNode node = (AttributeAssignNode) statement;
                compile(node.value);
                int constant = chunk().addConstant(new Value(node.name.getValue().toString()));
                emit(ByteCodeOpCode.SetAttr, constant);
            }
            case ATTRIBUTE_ACCESS -> {
                AttributeAccessNode node = (AttributeAccessNode) statement;
                String attr = node.name.getValue().toString();
                int constant = chunk().addConstant(new Value(attr));
                emit(ByteCodeOpCode.GetAttr, constant);
            }
            case VAR_ASSIGNMENT -> compile((VarAssignNode) statement);
            case DYNAMIC_ASSIGN -> compile((MacroAssignNode) statement);
            case LET -> compile((LetNode) statement);
            case VAR_ACCESS -> compile((VarAccessNode) statement);
            case DROP -> compile((DropNode) statement);
            case THROW -> compile((ThrowNode) statement);
            case ASSERT -> compile((AssertNode) statement);


            // This is an if statement
            case QUERY -> compile((QueryNode) statement);
            case SWITCH -> compile((SwitchNode) statement);
            case PATTERN -> compile((PatternNode) statement);
            case WHILE -> compile((WhileNode) statement);
            case FOR -> compile((ForNode) statement);
            case ITERATOR -> compile((IterNode) statement);
            case BREAK -> {
                if (breaks.isEmpty())
                    error("Invalid Syntax", "Break statement outside of loop");
                compileNull();
                breaks.peek().add(emitJump(ByteCodeOpCode.Jump));
            }
            case CONTINUE -> emitLoop(continueTo.peek());
            case REFERENCE -> compile((RefNode) statement);
            case DE_REF -> compile((DerefNode) statement);
            case COMPILER -> compile((CompilerNode) statement);
            case MACRO_DEFINITION -> compile((MacroDefinitionNode) statement);
            default -> throw new RuntimeException("Unknown statement type: " + statement.getNodeType());
        }

        return typeHandler.resolve(statement);
    }

    void compile(MacroDefinitionNode macroDefinitionNode) {
        compileNull();
    }

    void compile(CompilerNode compilerNode) {
        compileNull();
    }

    void compile(DecoratorNode node) {
        Type decorated = compile(node.decorated);
        Type decorator = compile(node.decorator);
        if (!(decorator instanceof ClassObjectType) || !(decorated instanceof ClassObjectType)) {
            error("Decorator", "Decorator and decorated must be a inline");
        }
        Type result = decorator.call(new Type[]{decorated}, new Type[0]);
        if (!decorated.equals(result)) {
            error("Decorator", "Decorator must return the decorated inline");
        }
        emit(new int[]{
                ByteCodeOpCode.Call,
                1, 0
        });
        String name = node.name.getValue().toString();
        int arg = resolveLocal(name);

        if (arg != -1) {
            emit(ByteCodeOpCode.SetLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            emit(ByteCodeOpCode.SetUpvalue, arg);
        } else {
            arg = chunk().addConstant(new Value(name));
            emit(ByteCodeOpCode.SetGlobal, arg);
        }
    }

    void compile(PatternNode node) {
        Type target = compile(node.accessNode);
        if (!(target instanceof ClassType) && !(target instanceof EnumChildType)) {
            error("Pattern", "Pattern must be a class or enum");
        }
        inPattern = true;
        Token[] keySet = node.patterns.keySet().toArray(new Token[0]);
        for (Token token : keySet) {
            String name = token.getValue().toString();
            patternType = target.accessInternal(name);
            if (patternType == null) {
                error("Pattern", "Attribute does not exist");
            }
            compile(node.patterns.get(token));
        }
        inPattern = false;
        patternType = Types.VOID;
        emit(ByteCodeOpCode.Pattern, keySet.length);
        for (int i = keySet.length - 1; i >= 0; i--) {
            Token token = keySet[i];
            int constant = chunk().addConstant(new Value(token.getValue().toString()));
            emit(constant);
        }
    }

    void compile(DestructNode node) {
        Type destructed = compile(node.target);

        if (destructed instanceof NamespaceType)
            System.out.println(destructed);

        emit(ByteCodeOpCode.Destruct, node.subs.size());
        for (Token sub : node.subs) {
            String name = sub.getValue().toString();
            Type type = destructed.access(name);
            if (type == null) {
                error("Attribute", "Cannot access " + name + " in " + destructed);
            }
            globals.put(name, type);
            emit(chunk().addConstant(new Value(name)));
        }
    }

    void compile(UseNode node) {

        int code;
        int argc;
        String name;

        switch (name = node.useToken.getValue().toString()) {
            case "optimize" -> {
                argc = 0;
                code = HeadCode.OPTIMIZE;
            }
            case "func" -> {
                argc = 1;
                code = HeadCode.MAIN_FUNCTION;
            }
            case "main" -> {
                argc = 1;
                code = HeadCode.MAIN_CLASS;
            }
            case "export" -> {
                argc = -1;
                code = HeadCode.EXPORT;
            }
            case "package" -> {
                argc = -1;
                StringBuilder sb = new StringBuilder();
                for (Token token : node.args) {
                    sb.append(token.asString()).append(".");
                }
                chunk().packageName = sb.substring(0, sb.length() - 1);
                code = HeadCode.PACKAGE;
            }
            case "exportTo" -> {
                argc = 1;
                if (node.args.size() == 1) {
                    chunk().target = node.args.get(0).asString();
                }
                code = HeadCode.EXPORT_TO;
            }
            default -> {
                code = -1;
                argc = -1;
            }
        }

        if (code == -1) {
            error("Header Type", "Header does not exist");
        }

        if (node.args.size() != argc && argc != -1) {
            error("Argument Count", name + "() takes exactly " + argc + " arguments");
        }

        emit(ByteCodeOpCode.Header);
        emit(code, node.args.size());
        for (Token arg : node.args) {
            int constant = chunk().addConstant(new Value(arg.getValue().toString()));
            emit(constant);
        }
    }

    void compile(MacroAssignNode node) {
        macros.put(
                node.name.getValue().toString(),
                node.value
        );
        macroTypes.put(
                node.name.getValue().toString(),
                typeHandler.resolve(node.value)
        );
        compileNull();
    }

    void compile(BytesNode node) {
        compile(node.toBytes);
        emit(ByteCodeOpCode.ToBytes);
    }

    void compile(DerefNode node) {
        Type type = compile(node.ref);
        if (!(type instanceof ReferenceType)) {
            error("Type", "Cannot dereference " + type);
        }
        emit(ByteCodeOpCode.Deref);
    }

    void compile(RefNode node) {
        compile(node.inner);
        emit(ByteCodeOpCode.Ref);
    }

    void compile(SpreadNode node) {
        compile(node.internal);
        emit(ByteCodeOpCode.Spread);
    }

    Type compile(EnumNode node) {
        Map<String, LanguageEnumChild> children = new HashMap<>();
        EnumChildType[] types = new EnumChildType[node.children.size()];

        int argc = node.children.size();
        for (int i = 0; i < argc; i++) {
            EnumChild child = node.children.get(i);

            String name = child.token().getValue().toString();
            children.put(name, new LanguageEnumChild(
                    i,
                    child.params()
            ));

            GenericType[] generics = new GenericType[child.generics().size()];
            Set<String> removeLater = new HashSet<>();
            for (int j = 0; j < generics.length; j++) {
                generics[j] = new GenericType(child.generics().get(j));
                if (!typeHandler.types.containsKey(generics[j].name)) {
                    removeLater.add(generics[j].name);
                    typeHandler.types.put(generics[j].name, generics[j]);
                }
            }

            String[] properties = child.params().toArray(new String[0]);
            Type[] propertyTypes = new Type[child.types().size()];
            for (int j = 0; j < propertyTypes.length; j++) {
                Type type = typeLookup0(child.types().get(j));
                if (type == null) {
                    error("Type", "Type does not exist");
                }
                propertyTypes[j] = type;
            }

            EnumChildType type = new EnumChildType(name, propertyTypes, generics, properties);
            types[i] = type;

            for (String generic : removeLater) {
                typeHandler.types.remove(generic);
            }

            if (node.pub)
                globals.put(name, type);
        }

        String name = node.tok.getValue().toString();
        EnumType type = new EnumType(name, types);
        globals.put(name, type);
        int constant = chunk().addConstant(new Value(new LanguageEnum(
                name,
                children
        )));
        emit(new int[]{ByteCodeOpCode.Enum, constant, node.pub ? 1 : 0});
        return type;
    }

    private Type typeLookup(Token token) {
        return typeLookup0(WrappedCast.cast(token.getValue()));
    }

    private Type typeLookup0(List<String> strings) {
        return typeHandler.resolve(strings);
    }


    Type variableType(String toString) {
        int index;
        if (hasGlobal(toString)) {
            return getGlobal(toString);
        } else if ((index = resolveLocal(toString)) != -1) {
            return locals[index].type;
        } else if ((index = resolveUpValue(toString)) != -1) {
            return upValues[index].type;
        } else if (macroTypes.containsKey(toString)) {
            return macroTypes.get(toString);
        } else if (accessEnclosed(toString) != null) {
            return accessEnclosed(toString);
        }
        error("Scope", "Variable " + toString + " does not exist");
        return null;
    }

    void compile(AssertNode node) {
        compile(node.condition);
        emit(ByteCodeOpCode.Assert);
    }

    void compile(ThrowNode node) {
        compile(node.thrown);
        compile(node.throwType);
        emit(ByteCodeOpCode.Throw);
    }

    void compile(ReturnNode node) {
        if (node.nodeToReturn != null) {
            compile(node.nodeToReturn);
        } else {
            compileNull();
        }
        emit(ByteCodeOpCode.Return);
    }

    void compile(CallNode node) {
        int argc = node.argNodes.size();
        int argumentCount = node.kwargs.size();
        Type[] argTypes = new Type[argc];
        for (int i = 0; i < argc; i++) {
            argTypes[i] = compile(node.argNodes.get(i));
        }
        List<String> argumentNames = new ArrayList<>(node.kwargs.keySet());
        for (int i = argumentCount - 1; i >= 0; i--) {
            compile(node.kwargs.get(argumentNames.get(i)));
        }
        Type function = compile(node.nodeToCall);
        if (!function.callable()) {
            error("Type", "Can't call non-inline");
        }
        emit(new int[]{
                ByteCodeOpCode.Call,
                argc, argumentCount
        });
        for (int i = 0; i < argumentCount; i++) {
            emit(chunk().addConstant(new Value(argumentNames.get(i))));
        }
        Type[] generics = new Type[node.generics.size()];
        for (int i = 0; i < generics.length; i++) {
            Token generic = node.generics.get(i);
            Type type = typeLookup(generic);
            generics[i] = type;
        }

        Type res = function.call(argTypes, generics);
        if (res == null) {
            Type[] elements = function instanceof ClassObjectType ?
                    ((ClassObjectType) function).parameterTypes :
                    function instanceof ClassType ? new Type[]{function} : null;

            error(
                    "Can't call function with given arguments",
                    "argument given: " + Arrays.toString(argTypes)
                            + " required: " + Arrays.toString(elements));
        }
    }

    void compile(InlineDeclareNode node) {
        ClassObjectType type = (ClassObjectType) typeHandler.resolve(node);

        int global = -1;
        if (node.name != null) {
            global = parseVariable(node.name, type);
            markInitialized();
            if (scopeDepth == 0)
                globals.put(node.name.getValue().toString(), type);
        }

        function(FunctionType.FUNCTION, type, node);

        if (node.name != null) {
            defineVariable(global, type, false);
        }
    }

    void compile(SwitchNode node) {
        wrapScope(compiler -> {
                    if (node.match)
                        compiler.compileMatch(node);
                    else
                        compiler.compileSwitch(node);
                },
                null);
    }

    void compileSwitch(SwitchNode node) {
        breaks.add(new ArrayList<>());
        int[] jumps = new int[node.cases.size()];
        for (int i = 0; i < jumps.length; i++) {
            Case elementCase = node.cases.get(i);

            compile(node.reference);
            compile(elementCase.getCondition());
            emit(ByteCodeOpCode.EQUAL);

            jumps[i] = emitJump(ByteCodeOpCode.JumpIfTrue);
            emit(ByteCodeOpCode.Pop);
        }
        int defaultJump = emitJump(ByteCodeOpCode.Jump);

        for (int i = 0; i < jumps.length; i++) {
            Case elementCase = node.cases.get(i);
            int jump = jumps[i];

            patchJump(jump);
            emit(ByteCodeOpCode.Pop);
            compile(elementCase.getStatements());
        }

        patchJump(defaultJump);
        if (node.elseCase != null)
            compile(node.elseCase.getStatements());

        patchBreaks();

        compileNull();

    }

    void compileMatch(SwitchNode node) {
        breaks.add(new ArrayList<>());
        int[] jumps = new int[node.cases.size()];
        for (int i = 0; i < jumps.length; i++) {
            Case elementCase = node.cases.get(i);

            compile(node.reference);
            int height = localCount;
            compile(elementCase.getCondition());
            emit(ByteCodeOpCode.EQUAL);

            int jump = emitJump(ByteCodeOpCode.JumpIfFalse);
            emit(ByteCodeOpCode.Pop);

            compile(elementCase.getStatements());
            jumps[i] = emitJump(ByteCodeOpCode.Jump);
            patchJump(jump);

            emit(ByteCodeOpCode.Pop);

            localCount = height;
        }

        if (node.elseCase != null) {
            compile(node.elseCase.getStatements());
        } else {
            compileNull();
        }

        for (int jump : jumps)
            patchJump(jump);

        patchBreaks();
    }

    void markInitialized() {
        if (scopeDepth == 0) return;
        locals[localCount - 1].depth = scopeDepth;
    }

    void function(FunctionType type, ClassObjectType classObjectType, InlineDeclareNode node) {
        function(type, classObjectType, node, c -> {
        }, c -> {
        });
    }

    void function(FunctionType type, ClassObjectType classObjectType, InlineDeclareNode node, CompilerWrapped pre, CompilerWrapped post) {
        Compiler compiler = new Compiler(this, type, chunk().source, classObjectType).catchErrors(node.catcher);
        compiler.beginScope();

        for (GenericType generic : classObjectType.generics) {
            compiler.typeHandler.types.put(generic.name, generic);
        }

        for (int i = 0; i < node.argumentNames.size(); i++) {
            compiler.byteCode.arity++;
            compiler.byteCode.totarity++;

            Token param = node.argumentNames.get(i);
            Token paramType = node.argumentTypes.get(i);

            compiler.parseVariable(param, compiler.typeLookup(paramType));
            compiler.makeVar(compiler.localCount - 1, false);
        }

        if (node.argname != null) {
            Token argNameToken = new Token(TokenType.IDENTIFIER, node.argname, Position.EMPTY, Position.EMPTY);
            compiler.byteCode.totarity++;
            compiler.parseVariable(argNameToken, Types.LIST);
            compiler.makeVar(compiler.localCount - 1, false);
        }

        if (node.kwargname != null) {
            Token keywordToken = new Token(TokenType.IDENTIFIER, node.kwargname, Position.EMPTY, Position.EMPTY);
            compiler.byteCode.totarity++;
            compiler.parseVariable(keywordToken, Types.MAP);
            compiler.makeVar(compiler.localCount - 1, false);
        }

        pre.compile(compiler);
        compiler.compile(node.body);
        post.compile(compiler);

        compiler.emit(ByteCodeOpCode.Return);

        ByteCode byteCode = compiler.endCompiler();

        byteCode.name = node.name != null ? node.name.getValue().toString() : "<anonymous>";
        byteCode.async = node.async;

        byteCode.catcher = node.catcher;

        byteCode.varargs = node.argname != null;
        byteCode.kwargs = node.kwargname != null;

        for (Node defaultValue : node.defaults) {
            if (defaultValue != null)
                compile(defaultValue);
        }

        emit(new int[]{ByteCodeOpCode.Closure, chunk().addConstant(new Value(byteCode)), node.defaultCount});

        for (int i = 0; i < byteCode.upvalueCount; i++) {
            UpValue upvalue = compiler.upValues[i];

            emit(upvalue.isLocal ? 1 : upvalue.isGlobal ? 2 : 0);
            if (!upvalue.isGlobal) {
                emit(upvalue.index);
            } else {
                emit(chunk().addConstant(new Value(upvalue.globalName)));
            }
        }
    }

    private Compiler catchErrors(boolean catcher) {
        return this;
    }

    void compileNull() {
        emit(ByteCodeOpCode.Null);
    }

    void compileBoolean(boolean val) {
        int constant = chunk().addConstant(new Value(val));
        emit(ByteCodeOpCode.Constant, constant);
    }

    void compileNumber(double val) {
        int constant = chunk().addConstant(new Value(val));
        emit(ByteCodeOpCode.Constant, constant);
    }

    void compileString(String val) {
        int constant = chunk().addConstant(new Value(val));
        emit(ByteCodeOpCode.Constant, constant);
    }

    void compile(BinOpNode node) {
        if (node.operation == TokenType.AMPERSAND) {
            compile(node.left);
            int jump = emitJump(ByteCodeOpCode.JumpIfFalse);
            //noinspection DuplicatedCode
            emit(ByteCodeOpCode.Pop);
            compile(node.right);
            patchJump(jump);
            return;
        } else if (node.operation == TokenType.PIPE) {
            compile(node.left);
            int jump = emitJump(ByteCodeOpCode.JumpIfTrue);
            //noinspection DuplicatedCode
            emit(ByteCodeOpCode.Pop);
            compile(node.right);
            patchJump(jump);
            return;
        } else if (node.operation == TokenType.FAT_ARROW) {
            compile(node.right);
            compile(node.left);
            emit(ByteCodeOpCode.SetRef);
            return;
        } else if (node.operation == TokenType.COLON) {
            compile(node.left);
            compile(node.right);
            emit(ByteCodeOpCode.Chain);
            return;
        }

        compile(node.left);
        compile(node.right);
        switch (node.operation) {
            case PLUS -> emit(ByteCodeOpCode.Add);
            case MINUS -> emit(ByteCodeOpCode.Subtract);
            case STAR -> emit(ByteCodeOpCode.Multiply);
            case SLASH -> emit(ByteCodeOpCode.Divide);
            case PERCENT -> emit(ByteCodeOpCode.Modulo);
            case CARET -> emit(ByteCodeOpCode.Power);
            case EQUAL_EQUAL -> emit(ByteCodeOpCode.EQUAL);
            case BANG_EQUAL -> emit(new int[]{ByteCodeOpCode.EQUAL, ByteCodeOpCode.Not});
            case RIGHT_ANGLE -> emit(ByteCodeOpCode.GreaterThan);
            case LEFT_ANGLE -> emit(ByteCodeOpCode.LessThan);
            case GREATER_EQUALS -> emit(new int[]{ByteCodeOpCode.LessThan, ByteCodeOpCode.Not});
            case LESS_EQUALS -> emit(new int[]{ByteCodeOpCode.GreaterThan, ByteCodeOpCode.Not});
            case LEFT_BRACKET -> emit(ByteCodeOpCode.Index);
            case DOT -> emit(ByteCodeOpCode.Get);
            case TILDE_AMPERSAND -> emit(ByteCodeOpCode.BitAnd);
            case TILDE_PIPE -> emit(ByteCodeOpCode.BitOr);
            case TILDE_CARET -> emit(ByteCodeOpCode.BitXor);
            case LEFT_TILDE_ARROW -> emit(ByteCodeOpCode.LeftShift);
            case TILDE_TILDE -> emit(ByteCodeOpCode.RightShift);
            case RIGHT_TILDE_ARROW -> emit(ByteCodeOpCode.SignRightShift);
            default -> throw new RuntimeException("Unknown operator: " + node.operation);
        }
    }

    void compile(UnaryOpNode node) {
        compile(node.node);
        switch (node.operation) {
            case PLUS:
                break;
            case MINUS:
                emit(ByteCodeOpCode.Negate);
                break;
            case BANG:
                emit(ByteCodeOpCode.Not);
                break;
            case PLUS_PLUS:
                emit(ByteCodeOpCode.Increment);
                break;
            case MINUS_MINUS:
                emit(ByteCodeOpCode.Decrement);
                break;
            case TILDE:
                emit(ByteCodeOpCode.BitCompl);
                break;
            case DOLLAR:
                emit(ByteCodeOpCode.FromBytes);
                break;
            default:
                throw new RuntimeException("Unknown operator: " + node.operation);
        }
    }

    void compile(VarAccessNode node) {
        String name = node.name.getValue().toString();

        accessVariable(name);
    }

    void accessVariable(String name) {
        if (macros.containsKey(name)) {
            compile(macros.get(name));
            return;
        }

        int arg = resolveLocal(name);

        if (arg != -1) {
            emit(ByteCodeOpCode.GetLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            emit(ByteCodeOpCode.GetUpvalue, arg);
        } else if (hasGlobal(name)) {
            arg = chunk().addConstant(new Value(name));
            emit(ByteCodeOpCode.GetGlobal, arg);
        } else if (accessEnclosed(name) != null) {
            arg = chunk().addConstant(new Value(name));
            emit(ByteCodeOpCode.GetAttr, arg);
        } else if (inPattern) {
            arg = chunk().addConstant(new Value(name));
            addLocal(name, patternType);
            emit(ByteCodeOpCode.PatternVars, arg);
        } else {
            error("Scope", "Undefined variable '" + name + "'");
        }
    }

    void compile(DropNode node) {
        String name = node.varTok.getValue().toString();
        if (macros.containsKey(name)) {
            macros.remove(name);
            macroTypes.remove(name);
            return;
        }

        int arg = resolveLocal(name);

        if (arg != -1) {
            locals[arg] = null;
            emit(ByteCodeOpCode.DropLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            upValues[arg] = null;
            emit(ByteCodeOpCode.DropUpvalue, arg);
        } else {
            arg = chunk().addConstant(new Value(name));
            globals.remove(name);
            emit(ByteCodeOpCode.DropGlobal, arg);
        }

        compileNull();
    }

    void compile(VarAssignNode node) {
        if (node.defining)
            compileDeclaration(node.name,
                    typeLookup0(node.type), node.locked, node.value,
                    node.min != null ? node.min : Integer.MIN_VALUE,
                    node.max != null ? node.max : Integer.MAX_VALUE);
        else
            compileAssign(node.name, node.value);
    }

    void compile(LetNode node) {
        compileDeclaration(node.name, null, false,
                node.value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    void defineVariable(int global, Type type, boolean constant) {
        defineVariable(global, type, constant, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    void defineVariable(int global, Type type, boolean constant, int min, int max) {
        boolean usesRange = min != Integer.MIN_VALUE || max != Integer.MAX_VALUE;
        if (scopeDepth > 0) {
            markInitialized();
            emit(ByteCodeOpCode.DefineLocal);
            emit(constant ? 1 : 0);
            emit(usesRange ? 1 : 0);
            if (usesRange) {
                emit(min, max);
            }
            return;
        }

        globals.put(chunk().constants.values.get(global).asString(), type);
        emit(ByteCodeOpCode.DefineGlobal, global);
        emit(constant ? 1 : 0);
        emit(usesRange ? 1 : 0);
        if (usesRange) {
            emit(min, max);
        }
    }

    @SuppressWarnings("all")
    void makeVar(int slot, boolean constant) {
        emit(ByteCodeOpCode.MakeVar, slot);
        emit(constant ? 1 : 0);
    }

    void addLocal(String name, Type type) {
        Local local = new Local(new LocalToken(name), type, scopeDepth);

        locals[localCount++] = local;
    }

    @SuppressWarnings("unused")
    void addGeneric(String name) {
        Local local = new Local(new LocalToken(name), new GenericType(name), scopeDepth);

        generics[localCount++] = local;
        locals[localCount - 1] = local;
    }

    void declareVariable(Token varNameTok, Type type) {
        if (scopeDepth == 0)
            return;

        String name = varNameTok.getValue().toString();
        addLocal(name, type);
    }

    int parseVariable(Token varNameTok, Type type) {
        declareVariable(varNameTok, type);
        if (scopeDepth > 0)
            return 0;

        return chunk().addConstant(new Value(varNameTok.getValue().toString()));
    }

    void compileDeclaration(Token varNameTok, Type type, boolean locked, Node value,
                            int min, int max) {
        Type t = compile(value);
        if (type == null) type = t;
        int global = parseVariable(varNameTok, type);
        defineVariable(global, type, locked, min, max);
    }


    public final Map<String, String> missMatchMap = new HashMap<>() {{

        put("f32", "i64");
        put("i64", "f32");
        put("i32", "i64");
        put("l64", "i64");
        put("i16", "i64");
        put("i8", "i64");

    }};

    // might be fucked up?
    public boolean canBeMissMatched(final String first, final String last) {
        return missMatchMap.entrySet().stream()
                .anyMatch(entry -> entry.getKey().equalsIgnoreCase(first) && entry.getValue().equalsIgnoreCase(last));
    }

    void compileAssign(Token varNameTok, Node value) {
        String name = varNameTok.getValue().toString();
        int arg = resolveLocal(name);

        Type expected = variableType(name);
        Type actual = compile(value);

        if (!expected.equals(actual) &&
                !canBeMissMatched(expected.name, actual.name)) {
            error("Type", "Expected " + expected + " but got " + actual);
        }

        if (arg != -1) {
            emit(ByteCodeOpCode.SetLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            emit(ByteCodeOpCode.SetUpvalue, arg);
        } else {
            arg = chunk().addConstant(new Value(name));
            emit(ByteCodeOpCode.SetGlobal, arg);
        }
    }

    interface CompilerWrapped {
        void compile(Compiler compiler);
    }

    void wrapScope(CompilerWrapped method, String scopeName) {
        // ()<> -> Any
        Compiler scope = new Compiler(this, FunctionType.SCOPE, chunk().source, new ClassObjectType(Types.ANY, new Type[0], new GenericType[0], false));

        scope.beginScope();
        method.compile(scope);
        scope.emit(ByteCodeOpCode.Return);
        scope.endScope();

        ByteCode func = scope.endCompiler();
        func.name = scopeName;

        emit(new int[]{ByteCodeOpCode.Closure, chunk().addConstant(new Value(func)), 0});
        for (int i = 0; i < func.upvalueCount; i++) {
            UpValue upvalue = scope.upValues[i];
            emit(upvalue.isLocal ? 1 : upvalue.isGlobal ? 2 : 0);
            if (!upvalue.isGlobal) {
                emit(upvalue.index);
            } else {
                emit(chunk().addConstant(new Value(upvalue.globalName)));
            }
        }
        emit(new int[]{ByteCodeOpCode.Call, 0, 0});
    }

    void compile(ScopeNode node) {
        wrapScope(compiler -> compiler.compile(node.statements), node.scopeName);
    }

    void compile(QueryNode node) {
        List<Integer> jumps = new ArrayList<>();
        int lastJump = 0;

        for (Case nodeCase : node.cases) {
            if (lastJump != 0) {
                patchJump(lastJump);
                emit(ByteCodeOpCode.Pop);
            }

            compile(nodeCase.getCondition());
            lastJump = emitJump(ByteCodeOpCode.JumpIfFalse);
            emit(ByteCodeOpCode.Pop);
            beginScope();
            compile(nodeCase.getStatements());
            endScope();

            if (!nodeCase.isReturnValue()) {
                emit(ByteCodeOpCode.Pop);
                compileNull();
            }

            jumps.add(emitJump(ByteCodeOpCode.Jump));
        }

        if (node.elseCase != null) {
            if (lastJump != 0) {
                patchJump(lastJump);
                emit(ByteCodeOpCode.Pop);
                lastJump = 0;
            }

            beginScope();
            compile(node.elseCase.getStatements());
            endScope();

            if (!node.elseCase.isReturnValue()) {
                emit(ByteCodeOpCode.Pop);
                compileNull();
            }
        }

        if (lastJump != 0) {
            emit(ByteCodeOpCode.Pop);
            patchJump(lastJump);
        }

        for (int jump : jumps)
            patchJump(jump);

    }

    void loopBody(Node body, boolean returnsNull, int loopStart) {
        breaks.add(new ArrayList<>());
        continueTo.push(loopStart);

        beginScope();
        compile(body);

        if (returnsNull) {
            emit(ByteCodeOpCode.Pop);
        } else {
            emit(ByteCodeOpCode.CollectLoop);
        }

        int popCount = deStack(locals) + deStack(generics);

        endScope();
        emitLoop(loopStart);
        int pastJump = emitJump(ByteCodeOpCode.Jump);

        continueTo.pop();
        patchBreaks();
        for (int i = 0; i < popCount; i++)
            emit(ByteCodeOpCode.Pop);
        patchJump(pastJump);
    }

    Type compile(ClassDefNode node) {
        String name = node.className.getValue().toString();

        Position constructorStart = node.make.getStartPosition();
        Position constructorEnd = node.make.getEndPosition();
        MethodDeclareNode constructor = new MethodDeclareNode(
                new Token(TokenType.IDENTIFIER, "<make>", constructorStart, constructorEnd),
                node.argumentNames,
                node.argumentTypes,
                node.make,
                false,
                false,
                false,
                Collections.singletonList("void"),
                node.defaults,
                node.defaultCount,
                node.generics,
                false,
                false,
                node.argname,
                node.kwargname
        );

        int nameConstant = chunk().addConstant(new Value(name));
        Type type = typeHandler.resolve(node);
        declareVariable(node.className, type);

        for (int i = node.attributes.size() - 1; i >= 0; i--) {
            AttributeDeclareNode attr = node.attributes.get(i);
            Node def = attr.nValue;
            if (def != null)
                compile(def);
            else
                compileNull();
        }

        enclosingType = type;

        for (int i = 0; i < node.generics.size(); i++)
            compileNull();

        if (node.parentToken != null) {
            accessVariable(node.parentToken.getValue().toString());
        }

        emit(ByteCodeOpCode.Class, nameConstant);
        emit(node.parentToken != null ? 1 : 0);

        emit(node.attributes.size() + node.generics.size());

        for (Token tok : node.generics)
            compile(new AttributeDeclareNode(
                    tok,
                    Collections.singletonList("String"),
                    false,
                    true,
                    null
            ));

        for (AttributeDeclareNode attr : node.attributes)
            compile(attr);

        emit(node.generics.size());
        for (Token tok : node.generics)
            emit(chunk().addConstant(new Value(tok.getValue().toString())));

        defineVariable(nameConstant, type, true);

        for (MethodDeclareNode method : node.methods) {
            staticContext = method.stat;
            compile(method);
        }

        compile(constructor, true);

        enclosingType = Types.VOID;

        emit(ByteCodeOpCode.Pop);
        compileNull();

        return type;
    }

    void compile(MethodDeclareNode node) {
        compile(node, false);
    }

    void compile(MethodDeclareNode node, boolean isConstructor) {
        String name = node.name.getValue().toString();
        int nameConstant = chunk().addConstant(new Value(name));

        FunctionType type = isConstructor ? FunctionType.CONSTRUCTOR : FunctionType.METHOD;

        InlineDeclareNode func = node.asFuncDef();
        ClassObjectType classObjectType = (ClassObjectType) typeHandler.resolve(func);

        function(type, classObjectType, func);

        emit(new int[]{
                ByteCodeOpCode.Method,
                nameConstant,
                node.stat ? 1 : 0,
                node.priv ? 1 : 0,
                node.bin ? 1 : 0,
        });
    }

    void compile(AttributeDeclareNode node) {
        int global = chunk().addConstant(new Value(node.name));
        emit(new int[]{
                global,
                node.isprivate ? 1 : 0,
                node.isstatic ? 1 : 0,
        });
    }

    // CollectLoop adds the previous value to the loop stack in the VM
    // FlushLoop turns the values in the loop stack into an array, and
    // pushes it onto the stack, clearing the loop stack
    void compile(WhileNode node) {
        boolean isDoWhile = node.conLast;

        if (!node.retnull) emit(ByteCodeOpCode.StartCache);

        int skipFirst = isDoWhile ? emitJump(ByteCodeOpCode.Jump) : -1;
        int loopStart = chunk().code.size();

        compile(node.condition);
        int jump = emitJump(ByteCodeOpCode.JumpIfFalse);
        emit(ByteCodeOpCode.Pop);

        if (isDoWhile)
            patchJump(skipFirst);

        loopBody(node.body, node.retnull, loopStart);
        patchJump(jump);
        emit(ByteCodeOpCode.Pop);

        if (node.retnull) {
            compileNull();
        } else {
            emit(ByteCodeOpCode.FlushLoop);
        }
    }

    void compile(ForNode node) {
        if (!node.retnull) emit(ByteCodeOpCode.StartCache);
        beginScope();

        Type startType = typeHandler.resolve(node.start);
        Type stepType;
        if (node.step != null) {
            stepType = typeHandler.resolve(node.step);
        } else {
            stepType = Types.INT;
        }

        String name = node.name.getValue().toString();
        copyVar(node.name, startType.isCompatible(TokenType.PLUS, stepType), node.start);

        int firstSkip = emitJump(ByteCodeOpCode.Jump);

        int loopStart = chunk().code.size();

        compile(node.end);
        if (node.step != null) {
            compile(node.step);
        } else {
            compileNumber(1);
        }

        emit(ByteCodeOpCode.For);
        emit(resolveLocal(name));

        emit(0xff);
        int jump = chunk().code.size() - 1;

        patchJump(firstSkip);
        loopBody(node.body, node.retnull, loopStart);

        endIterator(node.retnull, jump);
    }

    void compile(IterNode node) {
        if (!node.retnull) emit(ByteCodeOpCode.StartCache);
        beginScope();

        String name = node.name.getValue().toString();
        copyVar(new Token(
                TokenType.IDENTIFIER,
                "@" + name,
                node.name.getStartPosition(),
                node.name.getEndPosition()
        ), Types.LIST, node.iterable);
        compileDeclaration(node.name,
                Types.ANY,
                false,
                new NullNode(new Token(
                        TokenType.IDENTIFIER,
                        "null",
                        node.name.getStartPosition(),
                        node.name.getEndPosition()
                )),
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        emit(ByteCodeOpCode.Pop);

        int loopStart = chunk().code.size();

        emit(ByteCodeOpCode.Iter);
        emit(resolveLocal("@" + name), resolveLocal(name));

        emit(0xff);
        int jump = chunk().code.size() - 1;

        loopBody(node.body, node.retnull, loopStart);

        endIterator(node.retnull, jump);
    }

    void endIterator(boolean returnNull, int jump) {
        int offset = chunk().code.size() - jump - 1;
        chunk().code.set(jump, offset);
        endScope();

        if (returnNull) {
            compileNull();
        } else {
            emit(ByteCodeOpCode.FlushLoop);
        }
    }

    void copyVar(Token varNameTok, Type type, Node startValueNode) {
        int global = parseVariable(varNameTok, type);
        compile(startValueNode);
        emit(ByteCodeOpCode.Copy);
        defineVariable(global, type, false);
        emit(ByteCodeOpCode.Pop);
    }

    void compile(ListNode node) {
        int size = node.elements.size();
        for (int i = node.elements.size() - 1; i >= 0; i--)
            compile(node.elements.get(i));
        emit(ByteCodeOpCode.MakeArray, size);
    }

    void compile(MapNode node) {
        Set<Map.Entry<Node, Node>> entries = node.dict.entrySet();
        int size = entries.size();
        for (Map.Entry<Node, Node> entry : entries) {
            compile(entry.getKey());
            compile(entry.getValue());
        }
        emit(ByteCodeOpCode.MakeMap, size);
    }

}
