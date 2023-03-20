package language.backend.compiler;

import language.backend.compiler.bytecode.headers.HeadCode;
import language.backend.compiler.bytecode.types.GenericType;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.types.Types;
import language.backend.compiler.bytecode.types.objects.*;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.enums.LanguageEnum;
import language.backend.compiler.bytecode.values.enums.LanguageEnumChild;
import language.backend.compiler.bytecode.values.bytecode.ByteCode;
import language.frontend.parser.nodes.NodeType;
import language.frontend.parser.nodes.variables.TypeDefinitionNode;
import language.frontend.lexer.token.Position;
import language.frontend.lexer.token.Token;
import language.frontend.lexer.token.TokenType;
import language.vm.library.LibraryClassLoader;
import language.vm.library.NativeContext;
import dtool.logger.Logger;
import dtool.logger.ImplLogger;
import dtool.logger.errors.LanguageException;
import language.frontend.parser.nodes.Node;
import language.frontend.parser.nodes.cases.Case;
import language.frontend.parser.nodes.definitions.*;
import language.frontend.parser.nodes.expressions.*;
import language.frontend.parser.nodes.operations.BinOpNode;
import language.frontend.parser.nodes.operations.UnaryOpNode;
import language.frontend.parser.nodes.values.*;
import language.frontend.parser.nodes.variables.AttributeAccessNode;
import language.frontend.parser.nodes.variables.VarAccessNode;
import language.frontend.parser.units.EnumChild;
import language.utils.WrappedCast;
import language.vm.VirtualMachine;

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

    private final Disassembler disassembler;

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

        this.disassembler = new Disassembler("compiler");

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

        SYSTEM_LOGGER.fail(languageException);
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
            emit(OpCode.Pop);
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
        return globals.containsKey(name) || enclosing != null && enclosing.hasGlobal(name);
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
        emit(OpCode.Loop);

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

        for (Node statement : statements) {
            compile(statement);
            emit(OpCode.Pop);
        }

        emit(OpCode.Return);

        return endCompiler();
    }

    public ByteCode endCompiler() {
        if (SYSTEM_LOGGER.isDebugging())
            disassembler.disassembleChunk(chunk(),
                    byteCode.name != null ? byteCode.name : "<script>");

        byteCode.chunk.compile();

        if (SYSTEM_LOGGER.isDebugging())
            disassembler.finish();

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
            case FUNCTION_DEFINITION -> compile((FunctionDeclareNode) statement);
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
                    emit(OpCode.Pop);
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
                emit(OpCode.Access, constant);
            }
            case ATTRIBUTE_ASSIGN -> {
                AttributeAssignNode node = (AttributeAssignNode) statement;
                compile(node.value);
                int constant = chunk().addConstant(new Value(node.name.getValue().toString()));
                emit(OpCode.SetAttr, constant);
            }
            case ATTRIBUTE_ACCESS -> {
                AttributeAccessNode node = (AttributeAccessNode) statement;
                String attr = node.name.getValue().toString();
                int constant = chunk().addConstant(new Value(attr));
                emit(OpCode.GetAttr, constant);
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
                breaks.peek().add(emitJump(OpCode.Jump));
            }
            case CONTINUE -> emitLoop(continueTo.peek());
            case REFERENCE -> compile((RefNode) statement);
            case DE_REF -> compile((DerefNode) statement);
            case TYPE_DEFINITION -> compile((TypeDefinitionNode) statement);
            default -> throw new RuntimeException("Unknown statement type: " + statement.getNodeType());
        }

        return typeHandler.resolve(statement);
    }

    void compile(DecoratorNode node) {
        Type decorated = compile(node.decorated);
        Type decorator = compile(node.decorator);
        if (!(decorator instanceof ClassObjectType) || !(decorated instanceof ClassObjectType)) {
            error("Decorator", "Decorator and decorated must be a function");
        }
        Type result = decorator.call(new Type[]{decorated}, new Type[0]);
        if (!decorated.equals(result)) {
            error("Decorator", "Decorator must return the decorated function");
        }
        emit(new int[]{
                OpCode.Call,
                1, 0
        });
        String name = node.name.getValue().toString();
        int arg = resolveLocal(name);

        if (arg != -1) {
            emit(OpCode.SetLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            emit(OpCode.SetUpvalue, arg);
        } else {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.SetGlobal, arg);
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
        emit(OpCode.Pattern, keySet.length);
        for (int i = keySet.length - 1; i >= 0; i--) {
            Token token = keySet[i];
            int constant = chunk().addConstant(new Value(token.getValue().toString()));
            emit(constant);
        }
    }

    void compile(TypeDefinitionNode node) {
        emit(OpCode.TypeDef, 2);
        String typeName = node.getName().toString();
        String original = node.getType().toString();

        emit(chunk().addConstant(new Value(typeName)));

        emit(chunk().addConstant(new Value(original)));
    }

    void compile(DestructNode node) {
        Type destructed = compile(node.target);

        if (destructed instanceof NamespaceType)
            System.out.println(destructed);

        emit(OpCode.Destruct, node.subs.size());
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
                String packageName = sb.substring(0, sb.length() - 1);
                chunk().packageName = packageName;
                code = HeadCode.PACKAGE;
            }
            case "exportTo" -> {
                argc = 1;
                if (node.args.size() == 1) {
                    String target = node.args.get(0).asString();
                    chunk().target = target;
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

        emit(OpCode.Header);
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
        emit(OpCode.ToBytes);
    }

    void compile(DerefNode node) {
        Type type = compile(node.ref);
        if (!(type instanceof ReferenceType)) {
            error("Type", "Cannot dereference " + type);
        }
        emit(OpCode.Deref);
    }

    void compile(RefNode node) {
        compile(node.inner);
        emit(OpCode.Ref);
    }

    void compile(SpreadNode node) {
        compile(node.internal);
        emit(OpCode.Spread);
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
        emit(new int[]{OpCode.Enum, constant, node.pub ? 1 : 0});
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
        emit(OpCode.Assert);
    }

    void compile(ThrowNode node) {
        compile(node.thrown);
        compile(node.throwType);
        emit(OpCode.Throw);
    }

    void compile(ReturnNode node) {
        if (node.nodeToReturn != null) {
            compile(node.nodeToReturn);
        } else {
            compileNull();
        }
        emit(OpCode.Return);
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
            error("Type", "Can't call non-function");
        }
        emit(new int[]{
                OpCode.Call,
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

    void compile(FunctionDeclareNode node) {
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
            emit(OpCode.EQUAL);

            jumps[i] = emitJump(OpCode.JumpIfTrue);
            emit(OpCode.Pop);
        }
        int defaultJump = emitJump(OpCode.Jump);

        for (int i = 0; i < jumps.length; i++) {
            Case elementCase = node.cases.get(i);
            int jump = jumps[i];

            patchJump(jump);
            emit(OpCode.Pop);
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
            emit(OpCode.EQUAL);

            int jump = emitJump(OpCode.JumpIfFalse);
            emit(OpCode.Pop);

            compile(elementCase.getStatements());
            jumps[i] = emitJump(OpCode.Jump);
            patchJump(jump);

            emit(OpCode.Pop);

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

    void function(FunctionType type, ClassObjectType classObjectType, FunctionDeclareNode node) {
        function(type, classObjectType, node, c -> {
        }, c -> {
        });
    }

    void function(FunctionType type, ClassObjectType classObjectType, FunctionDeclareNode node, CompilerWrapped pre, CompilerWrapped post) {
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

        compiler.emit(OpCode.Return);

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

        emit(new int[]{OpCode.Closure, chunk().addConstant(new Value(byteCode)), node.defaultCount});

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
        emit(OpCode.Null);
    }

    void compileBoolean(boolean val) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant);
    }

    void compileNumber(double val) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant);
    }

    void compileString(String val) {
        int constant = chunk().addConstant(new Value(val));
        emit(OpCode.Constant, constant);
    }

    void compile(BinOpNode node) {
        if (node.operation == TokenType.AMPERSAND) {
            compile(node.left);
            int jump = emitJump(OpCode.JumpIfFalse);
            //noinspection DuplicatedCode
            emit(OpCode.Pop);
            compile(node.right);
            patchJump(jump);
            return;
        } else if (node.operation == TokenType.PIPE) {
            compile(node.left);
            int jump = emitJump(OpCode.JumpIfTrue);
            //noinspection DuplicatedCode
            emit(OpCode.Pop);
            compile(node.right);
            patchJump(jump);
            return;
        } else if (node.operation == TokenType.FAT_ARROW) {
            compile(node.right);
            compile(node.left);
            emit(OpCode.SetRef);
            return;
        } else if (node.operation == TokenType.COLON) {
            compile(node.left);
            compile(node.right);
            emit(OpCode.Chain);
            return;
        }

        compile(node.left);
        compile(node.right);
        switch (node.operation) {
            case PLUS -> emit(OpCode.Add);
            case MINUS -> emit(OpCode.Subtract);
            case STAR -> emit(OpCode.Multiply);
            case SLASH -> emit(OpCode.Divide);
            case PERCENT -> emit(OpCode.Modulo);
            case CARET -> emit(OpCode.Power);
            case EQUAL_EQUAL -> emit(OpCode.EQUAL);
            case BANG_EQUAL -> emit(new int[]{OpCode.EQUAL, OpCode.Not});
            case RIGHT_ANGLE -> emit(OpCode.GreaterThan);
            case LEFT_ANGLE -> emit(OpCode.LessThan);
            case GREATER_EQUALS -> emit(new int[]{OpCode.LessThan, OpCode.Not});
            case LESS_EQUALS -> emit(new int[]{OpCode.GreaterThan, OpCode.Not});
            case LEFT_BRACKET -> emit(OpCode.Index);
            case DOT -> emit(OpCode.Get);
            case TILDE_AMPERSAND -> emit(OpCode.BitAnd);
            case TILDE_PIPE -> emit(OpCode.BitOr);
            case TILDE_CARET -> emit(OpCode.BitXor);
            case LEFT_TILDE_ARROW -> emit(OpCode.LeftShift);
            case TILDE_TILDE -> emit(OpCode.RightShift);
            case RIGHT_TILDE_ARROW -> emit(OpCode.SignRightShift);
            default -> throw new RuntimeException("Unknown operator: " + node.operation);
        }
    }

    void compile(UnaryOpNode node) {
        compile(node.node);
        switch (node.operation) {
            case PLUS:
                break;
            case MINUS:
                emit(OpCode.Negate);
                break;
            case BANG:
                emit(OpCode.Not);
                break;
            case PLUS_PLUS:
                emit(OpCode.Increment);
                break;
            case MINUS_MINUS:
                emit(OpCode.Decrement);
                break;
            case TILDE:
                emit(OpCode.BitCompl);
                break;
            case DOLLAR:
                emit(OpCode.FromBytes);
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
            emit(OpCode.GetLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            emit(OpCode.GetUpvalue, arg);
        } else if (hasGlobal(name)) {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.GetGlobal, arg);
        } else if (accessEnclosed(name) != null) {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.GetAttr, arg);
        } else if (inPattern) {
            arg = chunk().addConstant(new Value(name));
            addLocal(name, patternType);
            emit(OpCode.PatternVars, arg);
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
            emit(OpCode.DropLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            upValues[arg] = null;
            emit(OpCode.DropUpvalue, arg);
        } else {
            arg = chunk().addConstant(new Value(name));
            globals.remove(name);
            emit(OpCode.DropGlobal, arg);
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
            emit(OpCode.DefineLocal);
            emit(constant ? 1 : 0);
            emit(usesRange ? 1 : 0);
            if (usesRange) {
                emit(min, max);
            }
            return;
        }

        globals.put(chunk().constants.values.get(global).asString(), type);
        emit(OpCode.DefineGlobal, global);
        emit(constant ? 1 : 0);
        emit(usesRange ? 1 : 0);
        if (usesRange) {
            emit(min, max);
        }
    }

    @SuppressWarnings("all")
    void makeVar(int slot, boolean constant) {
        emit(OpCode.MakeVar, slot);
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

        put("float", "double");
        put("double", "float");
        put("int", "double");
        put("long", "double");
        put("short", "double");
        put("byte", "double");

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
            emit(OpCode.SetLocal, arg);
        } else if ((arg = resolveUpValue(name)) != -1) {
            emit(OpCode.SetUpvalue, arg);
        } else {
            arg = chunk().addConstant(new Value(name));
            emit(OpCode.SetGlobal, arg);
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
        scope.emit(OpCode.Return);
        scope.endScope();

        ByteCode func = scope.endCompiler();
        func.name = scopeName;

        emit(new int[]{OpCode.Closure, chunk().addConstant(new Value(func)), 0});
        for (int i = 0; i < func.upvalueCount; i++) {
            UpValue upvalue = scope.upValues[i];
            emit(upvalue.isLocal ? 1 : upvalue.isGlobal ? 2 : 0);
            if (!upvalue.isGlobal) {
                emit(upvalue.index);
            } else {
                emit(chunk().addConstant(new Value(upvalue.globalName)));
            }
        }
        emit(new int[]{OpCode.Call, 0, 0});
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
                emit(OpCode.Pop);
            }

            compile(nodeCase.getCondition());
            lastJump = emitJump(OpCode.JumpIfFalse);
            emit(OpCode.Pop);
            beginScope();
            compile(nodeCase.getStatements());
            endScope();

            if (!nodeCase.isReturnValue()) {
                emit(OpCode.Pop);
                compileNull();
            }

            jumps.add(emitJump(OpCode.Jump));
        }

        if (node.elseCase != null) {
            if (lastJump != 0) {
                patchJump(lastJump);
                emit(OpCode.Pop);
                lastJump = 0;
            }

            beginScope();
            compile(node.elseCase.getStatements());
            endScope();

            if (!node.elseCase.isReturnValue()) {
                emit(OpCode.Pop);
                compileNull();
            }
        }

        if (lastJump != 0) {
            emit(OpCode.Pop);
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
            emit(OpCode.Pop);
        } else {
            emit(OpCode.CollectLoop);
        }

        int popCount = deStack(locals) + deStack(generics);

        endScope();
        emitLoop(loopStart);
        int pastJump = emitJump(OpCode.Jump);

        continueTo.pop();
        patchBreaks();
        for (int i = 0; i < popCount; i++)
            emit(OpCode.Pop);
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

        if (node.parentToken != null)
            accessVariable(node.parentToken.getValue().toString());

        emit(OpCode.Class, nameConstant);
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

        emit(OpCode.Pop);
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

        FunctionDeclareNode func = node.asFuncDef();
        ClassObjectType classObjectType = (ClassObjectType) typeHandler.resolve(func);

        function(type, classObjectType, func);

        emit(new int[]{
                OpCode.Method,
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

        if (!node.retnull) emit(OpCode.StartCache);

        int skipFirst = isDoWhile ? emitJump(OpCode.Jump) : -1;
        int loopStart = chunk().code.size();

        compile(node.condition);
        int jump = emitJump(OpCode.JumpIfFalse);
        emit(OpCode.Pop);

        if (isDoWhile)
            patchJump(skipFirst);

        loopBody(node.body, node.retnull, loopStart);
        patchJump(jump);
        emit(OpCode.Pop);

        if (node.retnull) {
            compileNull();
        } else {
            emit(OpCode.FlushLoop);
        }
    }

    void compile(ForNode node) {
        if (!node.retnull) emit(OpCode.StartCache);
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

        int firstSkip = emitJump(OpCode.Jump);

        int loopStart = chunk().code.size();

        compile(node.end);
        if (node.step != null) {
            compile(node.step);
        } else {
            compileNumber(1);
        }

        emit(OpCode.For);
        emit(resolveLocal(name));

        emit(0xff);
        int jump = chunk().code.size() - 1;

        patchJump(firstSkip);
        loopBody(node.body, node.retnull, loopStart);

        endIterator(node.retnull, jump);
    }

    void compile(IterNode node) {
        if (!node.retnull) emit(OpCode.StartCache);
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
        emit(OpCode.Pop);

        int loopStart = chunk().code.size();

        emit(OpCode.Iter);
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
            emit(OpCode.FlushLoop);
        }
    }

    void copyVar(Token varNameTok, Type type, Node startValueNode) {
        int global = parseVariable(varNameTok, type);
        compile(startValueNode);
        emit(OpCode.Copy);
        defineVariable(global, type, false);
        emit(OpCode.Pop);
    }

    void compile(ListNode node) {
        int size = node.elements.size();
        for (int i = node.elements.size() - 1; i >= 0; i--)
            compile(node.elements.get(i));
        emit(OpCode.MakeArray, size);
    }

    void compile(MapNode node) {
        Set<Map.Entry<Node, Node>> entries = node.dict.entrySet();
        int size = entries.size();
        for (Map.Entry<Node, Node> entry : entries) {
            compile(entry.getKey());
            compile(entry.getValue());
        }
        emit(OpCode.MakeMap, size);
    }

}
