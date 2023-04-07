package language.vm;

import dtool.logger.ImplLogger;
import dtool.logger.Logger;
import language.backend.compiler.bytecode.ByteCodeOpCode;
import language.backend.compiler.bytecode.CompilerStack;
import language.backend.compiler.bytecode.headers.HeadCode;
import language.backend.compiler.bytecode.headers.MemoCache;
import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.values.Pattern;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.Var;
import language.backend.compiler.bytecode.values.bytecode.*;
import language.backend.compiler.bytecode.values.classes.*;
import language.backend.compiler.bytecode.values.enums.LanguageEnum;
import language.backend.compiler.bytecode.values.enums.LanguageEnumChild;
import language.utils.Pair;
import language.vm.library.LibraryClassLoader;
import language.vm.library.NativeContext;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualMachine {

    static {
        LibraryClassLoader.LIBRARY_LOADER.indexLibraryClasses();
    }

    public static final Logger SYSTEM_LOGGER = ImplLogger.getInstance();

    public static final Map<String, Namespace> NATIVE_STD = new HashMap<>();
    public static final Map<String, Var> GLOBAL_VAR_ARGS = new HashMap<>();

    public static final int MAX_STACK_SIZE = 256;
    public static final int FRAMES_MAX = 256;

    private static final MemoCache MEMO_CACHE = new MemoCache();

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static VirtualMachine instance;

    private final Stack<List<Value>> loopCache;
    private Stack<Traceback> tracebacks;

    public boolean initialized = false;

    private final CompilerStack<Value> stack;

    private List<String> exports = null;

    private List<Value> currentLoop;

    private Pair<String, String> lastError;

    public CallFrame frame;
    public final CompilerStack<CallFrame> frames;

    private String mainFunction;
    private String mainClass;

    public boolean safe = false;
    public boolean failed = false;
    public boolean sim = false;

    public NativeResult res;

    public VirtualMachine(ByteCode byteCode) {
        this(byteCode, new HashMap<>());
    }

    public VirtualMachine(ByteCode byteCode, Map<String, Var> globals) {
        this(new Closure(byteCode), globals);
    }

    public VirtualMachine(Closure closure, Map<String, Var> globals) {
        instance = this;

        this.stack = new CompilerStack<>(MAX_STACK_SIZE);

        push(new Value(closure));

        // clean up
        GLOBAL_VAR_ARGS.clear();
        // copy globals
        GLOBAL_VAR_ARGS.putAll(globals);
        // add libraries
        GLOBAL_VAR_ARGS
                .putAll(LibraryClassLoader.LIBRARY_VAR_ARGS);

        this.tracebacks = new Stack<>();
        this.loopCache = new Stack<>();

        this.currentLoop = null;

        this.frames = new CompilerStack<>(FRAMES_MAX);
        this.frame = new CallFrame(closure, 0, 0, "void");
        frames.push(frame);

        NativeContext.init(this);

        initialized = true;
    }

    public static NativeResult run(Closure function, Value[] args) {
        if (function.byteCode.totarity != args.length) {
            return NativeResult.Err("Argument Count", "Expected " + function.byteCode.totarity + " arguments, got " + args.length);
        }
        VirtualMachine virtualMachine = new VirtualMachine(function, new HashMap<>());
        virtualMachine.sim = true;
        for (Value arg : args)
            virtualMachine.push(arg);
        virtualMachine.run();
        return virtualMachine.res;
    }

    public void defineNative(String name, Native.Method method, int argc) {
        GLOBAL_VAR_ARGS.put(name, new Var(
                new Value(new Native(name, method, argc)),
                true
        ));
    }

    public void defineNative(String library, String name, Native.Method method, int argc) {
        if (!NATIVE_STD.containsKey(library))
            NATIVE_STD.put(library, new Namespace(library, new HashMap<>()));
        NATIVE_STD.get(library).addField(name, new Value(
                new Native(name, method, argc)
        ));
    }

    public void defineVar(String lib, String name, Value val) {
        if (!NATIVE_STD.containsKey(lib))
            NATIVE_STD.put(lib, new Namespace(lib, new HashMap<>()));
        NATIVE_STD.get(lib).addField(name, val);
    }

    public void defineNative(String name, Native.Method method, Type[] types) {
        GLOBAL_VAR_ARGS.put(name, new Var(
                new Value(new Native(name, method, types.length, types)),
                true
        ));
    }

    public void defineNative(String library, String name, Native.Method method, Type[] types) {
        if (!NATIVE_STD.containsKey(library))
            NATIVE_STD.put(library, new Namespace(library, new HashMap<>()));
        NATIVE_STD.get(library).addField(name, new Value(
                new Native(name, method, types.length, types)
        ));
    }

    public VirtualMachine trace(String name) {
        tracebacks.push(new Traceback(name, name, 0, frame.closure.byteCode.chunk));
        return this;
    }

    private void popTraceback() {
        tracebacks.pop();
    }

    void moveIP(int offset) {
        frame.ip += offset;
    }

    public void push(Value value) {
        stack.push(value);
    }

    public Value pop() {
        return stack.pop();
    }

    Stack<Traceback> copyTracebacks() {
        Stack<Traceback> copy = new Stack<>();
        for (Traceback traceback : tracebacks)
            copy.push(traceback);
        return copy;
    }

    protected void runtimeError(String message, String reason) {

        lastError = new Pair<>(message, reason);
        if (sim) {
            res = NativeResult.Err(message, reason);
            return;
        }

        String output = "";

        Stack<Traceback> copy = copyTracebacks();

        //if (!tracebacks.empty()) {
        //    String arrow = "->";
        //
        //    // Generate traceback
        //    Traceback last = tracebacks.peek();
        //    while (last == null) {
        //        tracebacks.pop();
        //        last = tracebacks.peek();
        //    }
        //    while (!tracebacks.empty()) {
        //        Traceback top = tracebacks.pop();
        //        if (top == null) continue;        TODO RECODE THIS
        //        int line = indexToLine(top.chunk.source(), top.chunk.getPosition(top.offset).index);
        //        output = String.format("  %s  File %s, line %s, in %s\n%s", arrow, top.filename, line + 1, top.context, output);
        //    }
        //    output = "Traceback (most recent call last):\n" + output;
        //
        //    // Generate error message
        //    int line = indexToLine(frame.closure.byteCode.chunk.source(), idx);
        //    output += String.format("\n%s Error (Runtime): %s\nFile %s, line %s\n%s\n",
        //            message, reason,
        //            last.filename, line + 1,
        //            SyntaxHighlighter.highlightFlat(frame.closure.byteCode.chunk.source(), idx, len));
        //} else {
        //    output = String.format("%s Error (Runtime): %s\n", message, reason);
        //}

        tracebacks = copy;

        if (safe) {
            SYSTEM_LOGGER.warn(output);
        } else {
            while (frames.count > 0) {
                CallFrame frame = frames.pop();
                Traceback traceback = copy.pop();
                if (frame.catchError) {
                    frames.push(frame);
                    copy.push(traceback);
                    tracebacks = copy;
                    this.frame = frame;
                    return;
                }
            }
            System.err.println(output);
            System.exit(-1);
            resetStack();
        }
        failed = true;
    }

    void resetStack() {
        stack.clear();
        frames.clear();
    }

    String readString() {
        return readConstant().asString();
    }

    Value readConstant() {
        return frame.closure.byteCode.chunk.constants().valuesArray[readByte()];
    }

    int readByte() {
        return frame.closure.byteCode.chunk.codeArray[frame.ip++];
    }

    Value peek(int offset) {
        return stack.peek(offset);
    }

    int isFalsey(Value value) {
        return !value.asBool() ? 1 : 0;
    }

    VirtualMachineResult runBin(String name, Value arg, Instance instance) {
        return runBin(name, new Value[]{arg}, instance);
    }

    VirtualMachineResult runBin(String name, Value[] args, Instance instance) {
        Value method = instance.binMethods.get(name);

        push(method);
        for (Value arg : args) {
            push(arg);
        }

        BoundMethod bound = new BoundMethod(method.asClosure(), instance.self);
        if (!callValue(new Value(bound), args, new HashMap<>())) return VirtualMachineResult.ERROR;

        VirtualMachineResult res = run();
        return res != VirtualMachineResult.ERROR ? VirtualMachineResult.OK : VirtualMachineResult.ERROR;
    }

    boolean canOverride(Value value, String name) {
        return value.isInstance && value.asInstance().binMethods.containsKey(name);
    }

    VirtualMachineResult binary(int op) {
        Value b = pop();
        Value a = pop();

        switch (op) {
            case ByteCodeOpCode.Add -> {
                if (a.isString)
                    push(new Value(a.asString() + b.asString()));
                else if (a.isList) {
                    List<Value> list = new ArrayList<>(a.asList());
                    list.addAll(b.asList());
                    push(new Value(list));
                } else if (canOverride(a, "add"))
                    return runBin("add", b, a.asInstance());
                else
                    push(new Value(a.asNumber() + b.asNumber()));
            }
            case ByteCodeOpCode.Subtract -> {
                if (canOverride(a, "sub"))
                    return runBin("sub", b, a.asInstance());
                push(new Value(a.asNumber() - b.asNumber()));
            }
            case ByteCodeOpCode.Multiply -> {
                if (canOverride(a, "mul"))
                    return runBin("mul", b, a.asInstance());
                if (a.isString) {
                    push(new Value(repeat(a.asString(), b.asNumber().intValue())));
                } else if (a.isList) {
                    List<Value> repeated = new ArrayList<>();
                    List<Value> list = a.asList();
                    for (int i = 0; i < b.asNumber().intValue(); i++)
                        repeated.addAll(list);
                    push(new Value(repeated));
                } else {
                    push(new Value(a.asNumber() * b.asNumber()));
                }
            }
            case ByteCodeOpCode.Divide -> {
                if (canOverride(a, "div"))
                    return runBin("div", b, a.asInstance());
                else if (a.isList) {
                    List<Value> list = new ArrayList<>(a.asList());
                    list.remove(b);
                    push(new Value(list));
                } else
                    push(new Value(a.asNumber() / b.asNumber()));
            }
            case ByteCodeOpCode.Modulo -> {
                if (canOverride(a, "mod"))
                    return runBin("mod", b, a.asInstance());
                push(new Value(a.asNumber() % b.asNumber()));
            }
            case ByteCodeOpCode.Power -> {
                if (canOverride(a, "fastpow"))
                    return runBin("fastpow", b, a.asInstance());
                push(new Value(Math.pow(a.asNumber(), b.asNumber())));
            }
        }

        return VirtualMachineResult.OK;
    }

    VirtualMachineResult unary(int op) {
        Value a = pop();

        switch (op) {
            case ByteCodeOpCode.Increment -> push(new Value(a.asNumber() + 1));
            case ByteCodeOpCode.Decrement -> push(new Value(a.asNumber() - 1));
            case ByteCodeOpCode.Negate -> push(new Value(-a.asNumber()));
            case ByteCodeOpCode.Not -> push(new Value(!a.asBool()));
        }

        return VirtualMachineResult.OK;
    }

    VirtualMachineResult globalOps(int op) {
        switch (op) {
            case ByteCodeOpCode.DefineGlobal -> {
                String name = readString();
                Value value = peek(0);

                //noinspection DuplicatedCode
                boolean constant = readByte() == 1;

                boolean usesRange = readByte() == 1;
                int min = Integer.MIN_VALUE;
                int max = Integer.MAX_VALUE;
                if (usesRange) {
                    min = readByte();
                    max = readByte();
                }

                GLOBAL_VAR_ARGS.put(name, new Var(value, constant, min, max));
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.GetGlobal -> {
                String name = readString();
                Var value = GLOBAL_VAR_ARGS.get(name);

                if (value == null) {
                    VirtualMachineResult res = getBound(name, true);
                    if (res == VirtualMachineResult.OK)
                        return VirtualMachineResult.OK;
                    runtimeError("Scope", "Undefined variable");
                    return VirtualMachineResult.ERROR;
                }

                push(value.val);
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.SetGlobal -> {
                String name = readString();
                Value value = peek(0);

                VirtualMachineResult res = setBound(name, value, true);
                if (res == VirtualMachineResult.OK)
                    return VirtualMachineResult.OK;

                Var var = GLOBAL_VAR_ARGS.get(name);
                if (var != null) {
                    return set(var, value);
                } else {
                    runtimeError("Scope", "Undefined variable");
                    return VirtualMachineResult.ERROR;
                }
            }
            default -> {
                return VirtualMachineResult.OK;
            }
        }
    }

    VirtualMachineResult getBound(String name, boolean suppress) {
        if (frame.bound != null) {
            if (frame.bound.isInstance) {
                Instance instance = frame.bound.asInstance();
                Value field = instance.getField(name, true);
                if (field != null) {
                    push(field);
                    return VirtualMachineResult.OK;
                } else {
                    if (!suppress)
                        runtimeError("Scope", "Undefined attribute");
                    return VirtualMachineResult.ERROR;
                }
            } else if (frame.bound.isClass) {
                LanguageClass clazz = frame.bound.asClass();
                Value field = clazz.getField(name, true);
                if (field != null) {
                    push(field);
                    return VirtualMachineResult.OK;
                } else {
                    if (!suppress)
                        runtimeError("Scope", "Undefined attribute");
                    return VirtualMachineResult.ERROR;
                }
            } else if (frame.bound.isNamespace) {
                Namespace ns = frame.bound.asNamespace();
                Value field = ns.getField(name, true);
                if (field != null) {
                    push(field);
                    return VirtualMachineResult.OK;
                } else {
                    if (!suppress)
                        runtimeError("Scope", "Undefined attribute");
                    return VirtualMachineResult.ERROR;
                }
            }
            if (!suppress)
                runtimeError("Scope", "Not in class or instance");
            return VirtualMachineResult.ERROR;
        }
        if (!suppress)
            runtimeError("Scope", "Not in class or instance");
        return VirtualMachineResult.ERROR;
    }

    VirtualMachineResult setBound(String name, Value value, boolean suppress) {
        if (frame.bound != null) {
            if (frame.bound.isInstance) {
                Instance instance = frame.bound.asInstance();
                return boundNeutral(suppress, instance.setField(name, value));
            } else if (frame.bound.isClass) {
                LanguageClass clazz = frame.bound.asClass();
                return boundNeutral(suppress, clazz.setField(name, value));
            }
        }
        if (!suppress)
            runtimeError("Scope", "Not in class or instance");
        return VirtualMachineResult.ERROR;
    }

    VirtualMachineResult boundNeutral(boolean suppress, NativeResult nativeResult) {
        if (nativeResult.ok())
            return VirtualMachineResult.OK;
        else {
            if (!suppress)
                runtimeError(nativeResult.name(), nativeResult.reason());
            return VirtualMachineResult.ERROR;
        }
    }

    VirtualMachineResult attrOps(int op) {
        switch (op) {
            case ByteCodeOpCode.GetAttr -> {
                return getBound(readString(), false);
            }
            case ByteCodeOpCode.SetAttr -> {
                setBound(readString(), pop(), false);
                push(new Value());
                return VirtualMachineResult.OK;
            }
            default -> {
                return VirtualMachineResult.OK;
            }
        }
    }

    VirtualMachineResult comparison(int op) {
        Value b = pop();
        Value a = pop();

        switch (op) {
            case ByteCodeOpCode.EQUAL -> {
                if (b.isPattern) {
                    return matchPattern(a, b.asPattern());
                }
                if (canOverride(a, "eq")) {
                    return runBin("eq", b, a.asInstance());
                } else if (canOverride(b, "eq")) {
                    return runBin("eq", a, b.asInstance());
                }
                push(new Value(a.equals(b)));
            }
            case ByteCodeOpCode.GreaterThan -> {
                if (canOverride(b, "lte")) {
                    return runBin("lte", a, b.asInstance());
                }
                push(new Value(a.asNumber() > b.asNumber()));
            }
            case ByteCodeOpCode.LessThan -> {
                if (canOverride(a, "lt")) {
                    return runBin("lt", b, a.asInstance());
                }
                push(new Value(a.asNumber() < b.asNumber()));
            }
        }

        return VirtualMachineResult.OK;
    }

    private VirtualMachineResult matchPattern(Value a, Pattern asPattern) {
        if (!a.isInstance) {
            push(new Value(false));
            return VirtualMachineResult.OK;
        }

        Instance instance = a.asInstance();
        if (!instance.instanceOf(asPattern.value)) {
            push(new Value(false));
            return VirtualMachineResult.OK;
        }

        for (Map.Entry<String, Value> entry : asPattern.cases.entrySet()) {
            Value val = instance.getField(entry.getKey(), false);
            if (val == null) {
                push(new Value(false));
                return VirtualMachineResult.OK;
            } else if (!val.equals(entry.getValue())) {
                push(new Value(false));
                return VirtualMachineResult.OK;
            }
        }

        for (String binding : asPattern.keys) {
            Value val = instance.getField(binding, false);
            if (val == null) {
                runtimeError("Scope", "Undefined attribute");
                return VirtualMachineResult.ERROR;
            } else {
                push(new Value(new Var(val, true)));
            }
        }
        push(new Value(true));
        return VirtualMachineResult.OK;
    }

    Value get(int index) {
        return stack.get(index + frame.slots);
    }

    VirtualMachineResult set(Var var, Value val) {
        if (var.constant) {
            runtimeError("Scope", "Cannot reassign constant");
            return VirtualMachineResult.ERROR;
        }
        if (var.min != Integer.MIN_VALUE || var.max != Integer.MAX_VALUE) {
            if (val.isNumber) {
                double d = val.asNumber();
                if (d < var.min || d > var.max) {
                    runtimeError("Range", "Value out of range");
                    return VirtualMachineResult.ERROR;
                }
            } else {
                runtimeError("Range", "Value out of range");
                return VirtualMachineResult.ERROR;
            }
        }
        var.val(val);
        return VirtualMachineResult.OK;
    }

    VirtualMachineResult localOps(int op) {
        switch (op) {
            case ByteCodeOpCode.GetLocal -> {
                int slot = readByte();
                if (stack.count - slot <= 0) {
                    runtimeError("Scope", "Undefined variable");
                    return VirtualMachineResult.ERROR;
                }

                Value val = get(slot);
                push(val.asVar().val);
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.SetLocal -> {
                int slot = readByte();

                Value var = get(slot);
                Value val = peek(0);
                return set(var.asVar(), val);
            }
            case ByteCodeOpCode.DefineLocal -> {
                Value val = pop();
                //noinspection DuplicatedCode
                boolean constant = readByte() == 1;

                boolean usesRange = readByte() == 1;
                int min = Integer.MIN_VALUE;
                int max = Integer.MAX_VALUE;
                if (usesRange) {
                    min = readByte();
                    max = readByte();
                }

                Var var = new Var(val, constant, min, max);
                push(new Value(var));
                push(val);

                return VirtualMachineResult.OK;
            }
            default -> {
                return VirtualMachineResult.OK;
            }
        }
    }

    VirtualMachineResult loopOps(int op) {
        switch (op) {
            case ByteCodeOpCode.Loop -> {
                int offset = readByte();
                moveIP(-offset);
            }
            case ByteCodeOpCode.StartCache -> {
                currentLoop = new ArrayList<>();
                loopCache.push(currentLoop);
            }
            case ByteCodeOpCode.CollectLoop -> currentLoop.add(pop());
            case ByteCodeOpCode.FlushLoop -> {
                push(new Value(loopCache.pop()));
                if (loopCache.isEmpty())
                    currentLoop = null;
                else
                    currentLoop = loopCache.peek();
            }
        }

        return VirtualMachineResult.OK;
    }

    VirtualMachineResult jumpOps(int op) {
        int offset = switch (op) {
            case ByteCodeOpCode.JumpIfFalse -> readByte() * isFalsey(peek(0));
            case ByteCodeOpCode.JumpIfTrue -> readByte() * (1 - isFalsey(peek(0)));
            case ByteCodeOpCode.Jump -> readByte();
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
        moveIP(offset);
        return VirtualMachineResult.OK;
    }

    VirtualMachineResult forLoop() {
        double step = pop().asNumber();
        double end = pop().asNumber();

        int slot = readByte();
        int jump = readByte();

        get(slot).asVar().val(
                new Value(get(slot).asVar().val.asNumber() + step)
        );

        double i = get(slot).asVar().val.asNumber();
        moveIP(jump * (((i >= end && step >= 0) || (i <= end && step < 0)) ? 1 : 0));

        return VirtualMachineResult.OK;
    }

    VirtualMachineResult upvalueOps(int op) {
        switch (op) {
            case ByteCodeOpCode.GetUpvalue -> {
                int slot = readByte();
                if (frame.closure.upvalues[slot] == null) {
                    runtimeError("Scope", "Undefined variable");
                    return VirtualMachineResult.ERROR;
                }
                push(frame.closure.upvalues[slot].val);
            }
            case ByteCodeOpCode.SetUpvalue -> {
                int slot = readByte();
                return set(frame.closure.upvalues[slot], peek(0));
            }
        }

        return VirtualMachineResult.OK;
    }

    VirtualMachineResult byteOps(int op) {
        switch (op) {
            case ByteCodeOpCode.ToBytes -> {
                push(new Value(pop().asBytes()));
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.FromBytes -> {
                if (!peek(0).isBytes) {
                    runtimeError("Type", "Expected bytes");
                    return VirtualMachineResult.ERROR;
                }
                NativeResult res = Value.fromByte(pop().asBytes());
                if (res.ok()) {
                    push(res.value());
                    return VirtualMachineResult.OK;
                } else {
                    runtimeError(res.name(), res.reason());
                    return VirtualMachineResult.ERROR;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + op);
        }
    }

    VirtualMachineResult access() {
        String name = readString();
        Value val = pop();

        if (canOverride(val, "access")) {
            return runBin("access", new Value(name), val.asInstance());
        }

        if (val.isInstance) {
            return access(val, val.asInstance(), name);
        } else if (val.isClass) {
            return access(val, val.asClass(), name);
        } else if (val.isNamespace) {
            return access(val, val.asNamespace(), name);
        } else if (val.isEnumParent) {
            return access(val.asEnum(), name);
        }
        return VirtualMachineResult.ERROR;
    }

    VirtualMachineResult access(LanguageEnum languageEnum, String name) {
        push(languageEnum.get(name));
        return VirtualMachineResult.OK;
    }

    VirtualMachineResult access(Value val, Namespace namespace, String name) {
        return access(val, name, namespace.getField(name, false));
    }

    VirtualMachineResult access(Value val, Instance instance, String name) {
        return access(val, name, instance.getField(name, false));
    }

    VirtualMachineResult access(Value val, LanguageClass clazz, String name) {
        return access(val, name, clazz.getField(name, false));
    }

    VirtualMachineResult collections(int op) {
        switch (op) {
            case ByteCodeOpCode.Get, ByteCodeOpCode.Index -> {
                Value index = pop();
                Value collection = pop();

                if (collection.isList || collection.isString) {
                    List<Value> list = collection.asList();
                    int idx = index.asNumber().intValue();
                    if (idx >= list.size()) {
                        runtimeError("Index", "Index out of bounds");
                        return VirtualMachineResult.ERROR;
                    } else if (idx < 0) {
                        idx += list.size();
                        if (idx < 0) {
                            runtimeError("Index", "Index out of bounds");
                            return VirtualMachineResult.ERROR;
                        }
                    }
                    push(list.get(idx));
                } else if (canOverride(collection, op == ByteCodeOpCode.Get ? "get" : "bracket")) {
                    return runBin(op == ByteCodeOpCode.Get ? "get" : "bracket", index, collection.asInstance());
                } else if (collection.isMap) {
                    push(collection.get(index));
                }
                return VirtualMachineResult.OK;
            }
            default -> {
                return VirtualMachineResult.OK;
            }
        }
    }

    public static double bitOp(double left, double right, BitCall call) {
        long power = 0;
        while (left % 1 != 0 || right % 1 != 0) {
            left *= 10;
            right *= 10;
            power++;
        }
        return call.call((long) left, (long) right) / Math.pow(10, power);
    }

    VirtualMachineResult bitOps(int instruction) {
        switch (instruction) {
            case ByteCodeOpCode.BitAnd -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left & right)
                ));
            }
            case ByteCodeOpCode.BitOr -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left | right)
                ));
            }
            case ByteCodeOpCode.BitXor -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left ^ right)
                ));
            }
            case ByteCodeOpCode.LeftShift -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left << right)
                ));
            }
            case ByteCodeOpCode.RightShift -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left >>> right)
                ));
            }
            case ByteCodeOpCode.SignRightShift -> {
                Value b = pop();
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), b.asNumber(), (left, right) -> left >> right)
                ));
            }
            case ByteCodeOpCode.BitCompl -> {
                Value a = pop();
                push(new Value(
                        bitOp(a.asNumber(), 0, (left, right) -> ~left)
                ));
            }
        }
        return VirtualMachineResult.OK;
    }

    private VirtualMachineResult access(Value val, String name, Value member) {
        if (member == null) {
            runtimeError("Scope", "No member named " + name);
            return VirtualMachineResult.ERROR;
        }
        if (member.isClosure) {
            member = new Value(new BoundMethod(member.asClosure(), val));
        }
        push(member);
        return VirtualMachineResult.OK;
    }

    VirtualMachineResult call() {
        int argc = readByte();
        int kwargc = readByte();

        Value callee = pop();

        Map<String, Value> kwargs = new HashMap<>();
        for (int i = 0; i < kwargc; i++)
            kwargs.put(readString(), pop());

        Value[] args = new Value[argc];
        List<Value> argList = new ArrayList<>();
        for (int i = argc - 1; i >= 0; i--)
            args[i] = pop();

        push(callee);

        for (Value arg : args) {
            if (arg.isSpread) {
                Spread spread = arg.asSpread();
                for (Value val : spread.values) {
                    push(val);
                    argList.add(val);
                }
            } else {
                push(arg);
                argList.add(arg);
            }
        }

        // Stack:
        // [CALLEE] [GENERICS] [ARGUMENTS] [KWARGS]

        if (!callValue(callee, argList.toArray(new Value[0]), kwargs)) {
            return VirtualMachineResult.ERROR;
        }
        frame = frames.peek();
        return VirtualMachineResult.OK;
    }

    public boolean callValue(Value callee, Value[] args, Map<String, Value> kwargs) {
        if (callee.isNativeFunc) {
            return call(callee.asNative(), args);
        } else if (callee.isClosure) {
            return call(callee.asClosure(), args, kwargs);
        } else if (callee.isClass) {
            return call(callee.asClass(), args, kwargs);
        } else if (callee.isBoundMethod) {
            BoundMethod bound = callee.asBoundMethod();
            stack.set(stack.count - args.length - 1, new Value(new Var(bound.receiver, true)));
            return call(bound.closure, bound.receiver, args, kwargs);
        } else if (callee.isEnumChild) {
            return call(callee.asEnumChild(), args.length);
        }
        runtimeError("Type", "Can only call functions and classes");
        return false;
    }

    boolean call(LanguageEnumChild child, int argCount) {
        int argc = child.arity;

        if (argCount != argc) {
            runtimeError("Argument Count", "Expected " + argc + " but got " + argCount);
            return false;
        }

        Value[] args = new Value[argCount];
        for (int i = argCount - 1; i >= 0; i--)
            args[i] = pop();

        pop();
        push(child.create(args, this));
        return true;
    }

    boolean call(LanguageClass clazz, Value[] args, Map<String, Value> kwargs) {
        Instance instance = new Instance(clazz, this);

        Value value = new Value(instance);
        instance.self = value;

        Closure closure = clazz.constructor.asClosure();

        BoundMethod bound = new BoundMethod(closure, value);
        return callValue(new Value(bound), args, kwargs);
    }

    boolean call(Closure closure, Value[] args, Map<String, Value> kwargs) {
        return call(closure, frame.bound, args, kwargs);
    }

    public boolean call(Closure closure, Value binding, Value[] args, Map<String, Value> kwargs) {
        if (frame.optimization > 0) {
            Value val = MEMO_CACHE.get(closure.byteCode.name, args);
            if (val != null) {
                for (int i = 0; i <= args.length + kwargs.size(); i++)
                    pop();
                push(val);
                return true;
            }
            MEMO_CACHE.stackCache(closure.byteCode.name, args);
        }

        List<Value> extraArgs = new ArrayList<>();
        if (args.length < closure.byteCode.arity) {
            if (args.length + closure.byteCode.defaultCount < closure.byteCode.arity) {
                runtimeError("Argument Count", "Expected " + closure.byteCode.arity + " but got " + args.length);
                return false;
            }
            for (int i = args.length; i < closure.byteCode.arity; i++)
                push(closure.byteCode.defaults.get(i));
        } else if (args.length > closure.byteCode.arity) {
            if (closure.byteCode.varargs) {
                List<Value> argsList = new ArrayList<>();
                for (int i = closure.byteCode.arity; i < args.length; i++)
                    argsList.add(pop());
                for (int i = argsList.size() - 1; i >= 0; i--)
                    extraArgs.add(argsList.get(i));
                push(new Value(extraArgs));
            } else {
                runtimeError("Argument Count", "Expected " + closure.byteCode.arity + " but got " + args.length);
                return false;
            }
        }

        Map<Value, Value> keywordArgs = new HashMap<>();
        if (closure.byteCode.kwargs) {
            for (Map.Entry<String, Value> entry : kwargs.entrySet()) {
                String name = entry.getKey();
                keywordArgs.put(new Value(name), entry.getValue());
            }
            push(new Value(keywordArgs));
        }

        Traceback traceback = new Traceback(tracebacks.peek().filename, closure.byteCode.name, frame.ip - 1, frame.closure.byteCode.chunk);
        if (closure.byteCode.async) {
            VirtualMachine thread = new VirtualMachine(closure.byteCode.copy());
            thread.tracebacks.push(traceback);

            executorService.submit(() -> {
                thread.run();
            });
        } else {
            tracebacks.push(traceback);

            addFrame(closure, stack.count - closure.byteCode.totarity - 1, binding);
        }
        return true;
    }

    void addFrame(Closure closure, int slots, Value binding) {
        CallFrame newFrame = new CallFrame(closure, 0, slots, null, binding);

        // Inherited flags
        newFrame.optimization = frame.optimization > 0 ? 2 : 0;

        // General flags
        newFrame.catchError = closure.byteCode.catcher;

        frames.push(newFrame);

        frame = newFrame;
    }

    boolean call(Native nativeFunc, Value[] args) {
        NativeResult result = nativeFunc.call(args);

        if (!result.ok()) {
            runtimeError(result.name(), result.reason());
            return false;
        }

        stack.setTop(stack.count - args.length - 1);
        push(result.value());
        return true;
    }

    Var captureUpvalue(int slot) {
        return stack.get(slot).asVar();
    }

    void defineMethod(String name) {
        Value val = peek(0);
        LanguageClass clazz = peek(1).asClass();

        boolean isStatic = readByte() == 1;
        boolean isPrivate = readByte() == 1;
        boolean isBin = readByte() == 1;

        Closure method = val.asClosure();
        method.asMethod(isStatic, isPrivate, isBin, clazz.name);

        clazz.addMethod(name, val);
        pop();
    }

    VirtualMachineResult refOps(int op) {
        switch (op) {
            case ByteCodeOpCode.Ref -> {
                push(new Value(pop()));
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.Deref -> {
                if (!peek(0).isRef) {
                    runtimeError("Type", "Can't dereference non-ref");
                    return VirtualMachineResult.ERROR;
                }
                push(pop().asRef());
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.SetRef -> {
                if (!peek(0).isRef) {
                    runtimeError("Type", "Can't set non-ref");
                    return VirtualMachineResult.ERROR;
                }
                push(pop().setRef(pop()));
                return VirtualMachineResult.OK;
            }
            default -> throw new IllegalArgumentException("Invalid ref op: " + op);
        }
    }

    VirtualMachineResult freeOps(int op) {
        switch (op) {
            case ByteCodeOpCode.DropGlobal -> {
                String name = readString();
                if (GLOBAL_VAR_ARGS.containsKey(name)) {
                    GLOBAL_VAR_ARGS.remove(name);
                    return VirtualMachineResult.OK;
                } else {
                    runtimeError("Scope", "No such global: " + name);
                    return VirtualMachineResult.ERROR;
                }
            }
            case ByteCodeOpCode.DropLocal -> {
                int slot = readByte();
                if (slot + frame.slots == stack.count - 1) {
                    stack.pop();
                }
                stack.set(slot + frame.slots, null);
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.DropUpvalue -> {
                int slot = readByte();
                if (slot == frame.closure.upvalueCount - 1) {
                    frame.closure.upvalueCount--;
                }
                frame.closure.upvalues[slot] = null;
                return VirtualMachineResult.OK;
            }
            default -> throw new IllegalArgumentException("Invalid destructor op: " + op);
        }
    }

    VirtualMachineResult pattern(int op) {
        switch (op) {
            case ByteCodeOpCode.PatternVars -> {
                push(Value.patternBinding(readString()));
                return VirtualMachineResult.OK;
            }
            case ByteCodeOpCode.Pattern -> {
                int fieldCount = readByte();

                Map<String, Value> cases = new HashMap<>();
                Map<String, String> matches = new HashMap<>();
                List<String> keys = new ArrayList<>();
                for (int i = 0; i < fieldCount; i++) {
                    String name = readString();
                    Value val = pop();

                    if (val.isPatternBinding) {
                        matches.put(name, val.asPatternBinding());
                        keys.add(0, name);
                    } else {
                        cases.put(name, val);
                    }
                }

                Value pattern = pop();

                push(new Value(new Pattern(pattern, cases, keys.toArray(new String[0]), matches)));

                return VirtualMachineResult.OK;
            }
            default -> throw new IllegalArgumentException("How did you get here?");
        }
    }

    public VirtualMachineResult run() {
        frame = frames.peek();
        int exitLevel = frames.count - 1;

        while (true) {
            int instruction = readByte();
            VirtualMachineResult res;
            switch (instruction) {
                case ByteCodeOpCode.Return -> {
                    Value result = pop();
                    if (frame.catchError) result = new Value(new Result(result));
                    CallFrame frame = frames.pop();
                    if (frames.count == 0) {
                        if (sim)
                            this.res = NativeResult.Ok(result);
                        res = VirtualMachineResult.EXIT;
                        break;
                    }

                    boolean isConstructor = frame.closure.byteCode.name.equals("<make>");
                    Value bound = frame.bound;

                    stack.setTop(frame.slots);
                    this.frame = frames.peek();
                    popTraceback();

                    if (isConstructor) {
                        push(bound);
                    } else {
                        push(result);
                        if (frame.optimization == 2) {
                            MEMO_CACHE.storeCache(result);
                        }
                    }

                    if (exitLevel == frames.count) {
                        res = VirtualMachineResult.EXIT;
                        break;
                    }

                    if (frame.addPeek) {
                        this.frame.ip = frame.ip;
                    }

                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Constant -> {
                    push(readConstant());
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Assert -> {
                    Value value = pop();
                    if (isFalsey(value) == 1) {
                        runtimeError("Assertion", "Assertion failed");
                        res = VirtualMachineResult.ERROR;
                        break;
                    }
                    push(value);
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Pattern, ByteCodeOpCode.PatternVars -> res = pattern(instruction);
                case ByteCodeOpCode.Throw -> {
                    Value type = pop();
                    Value reason = pop();
                    runtimeError(type.asString(), reason.asString());
                    res = VirtualMachineResult.ERROR;
                }
                case ByteCodeOpCode.Import -> {
                    String name = readString();
                    String varName = readString();

                    Value f = pop();
                    if (!f.isFunc) {
                        if (!NATIVE_STD.containsKey(name)) {
                            runtimeError("Import", "Library '" + name + "' not found");
                            res = VirtualMachineResult.ERROR;
                            break;
                        }
                        Value lib = new Value(NATIVE_STD.get(name));
                        GLOBAL_VAR_ARGS.put(varName, new Var(
                                lib,
                                true
                        ));
                        push(lib);
                        res = VirtualMachineResult.OK;
                        break;
                    }

                    ByteCode func = f.asFunc();

                    VirtualMachine runner = new VirtualMachine(func);
                    runner.trace(name);
                    VirtualMachineResult importres = runner.run();
                    if (importres == VirtualMachineResult.ERROR) {
                        res = VirtualMachineResult.ERROR;
                        break;
                    }

                    Value space = new Value(runner.asNamespace(name));
                    GLOBAL_VAR_ARGS.put(varName, new Var(
                            space,
                            true
                    ));
                    push(space);
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Enum -> {
                    Value enumerator = readConstant();
                    GLOBAL_VAR_ARGS.put(enumerator.asEnum().name(), new Var(
                            enumerator,
                            true
                    ));
                    push(enumerator);

                    boolean isPublic = readByte() == 1;
                    if (isPublic) {
                        LanguageEnum enumObj = enumerator.asEnum();
                        for (Map.Entry<String, LanguageEnumChild> name : enumObj.children().entrySet()) {
                            GLOBAL_VAR_ARGS.put(name.getKey(), new Var(
                                    new Value(name.getValue()),
                                    true
                            ));
                        }
                    }

                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Copy -> {
                    push(pop().shallowCopy());
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Iter -> res = iterator();
                case ByteCodeOpCode.Spread -> {
                    push(new Value(new Spread(pop().asList())));
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Ref, ByteCodeOpCode.Deref, ByteCodeOpCode.SetRef -> res = refOps(instruction);
                case ByteCodeOpCode.Add, ByteCodeOpCode.Subtract, ByteCodeOpCode.Multiply, ByteCodeOpCode.Divide, ByteCodeOpCode.Modulo, ByteCodeOpCode.Power ->
                        res = binary(instruction);
                case ByteCodeOpCode.Increment, ByteCodeOpCode.Decrement, ByteCodeOpCode.Negate, ByteCodeOpCode.Not -> res = unary(instruction);
                case ByteCodeOpCode.FromBytes, ByteCodeOpCode.ToBytes -> res = byteOps(instruction);
                case ByteCodeOpCode.EQUAL, ByteCodeOpCode.GreaterThan, ByteCodeOpCode.LessThan -> res = comparison(instruction);
                case ByteCodeOpCode.Null -> {
                    push(new Value());
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Get, ByteCodeOpCode.Index -> res = collections(instruction);
                case ByteCodeOpCode.Pop -> {
                    pop();
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.DefineGlobal, ByteCodeOpCode.GetGlobal, ByteCodeOpCode.SetGlobal -> res = globalOps(instruction);
                case ByteCodeOpCode.GetLocal, ByteCodeOpCode.SetLocal, ByteCodeOpCode.DefineLocal -> res = localOps(instruction);
                case ByteCodeOpCode.JumpIfFalse, ByteCodeOpCode.JumpIfTrue, ByteCodeOpCode.Jump -> res = jumpOps(instruction);
                case ByteCodeOpCode.Loop, ByteCodeOpCode.StartCache, ByteCodeOpCode.CollectLoop, ByteCodeOpCode.FlushLoop -> res = loopOps(instruction);
                case ByteCodeOpCode.For -> res = forLoop();
                case ByteCodeOpCode.Call -> res = call();
                case ByteCodeOpCode.Closure -> {
                    ByteCode func = readConstant().asFunc();
                    int defaultCount = readByte();
                    Closure closure = new Closure(func);

                    if (func.name == null) func.name = tracebacks.peek().context;

                    Value[] defaults = new Value[func.arity];
                    for (int i = func.arity - 1; i >= func.arity - defaultCount; i--) {
                        defaults[i] = pop();
                        func.defaultCount++;
                    }
                    func.defaults = new ArrayList<>(Arrays.asList(defaults));

                    Value closed = new Value(closure);
                    push(closed);

                    for (int i = 0; i < closure.upvalueCount; i++) {
                        int isLocal = readByte();
                        int index = readByte();

                        switch (isLocal) {
                            case 0 -> closure.upvalues[i] = frame.closure.upvalues[index];
                            case 1 -> closure.upvalues[i] = captureUpvalue(frame.slots + index);
                            case 2 -> {
                                String name = frame.closure.byteCode.chunk.constants().valuesArray[index].asString();
                                if (Objects.equals(name, func.name)) {
                                    closure.upvalues[i] = new Var(closed, true);
                                } else {
                                    closure.upvalues[i] = GLOBAL_VAR_ARGS.get(name);
                                }
                            }
                        }
                    }

                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.GetAttr, ByteCodeOpCode.SetAttr -> res = attrOps(instruction);
                case ByteCodeOpCode.GetUpvalue, ByteCodeOpCode.SetUpvalue -> res = upvalueOps(instruction);
                case ByteCodeOpCode.BitAnd, ByteCodeOpCode.BitOr, ByteCodeOpCode.BitXor, ByteCodeOpCode.LeftShift, ByteCodeOpCode.RightShift, ByteCodeOpCode.SignRightShift, ByteCodeOpCode.BitCompl ->
                        res = bitOps(instruction);
                case ByteCodeOpCode.Chain -> {
                    Value b = pop();
                    Value a = pop();
                    if (a.isNull) {
                        push(b);
                    } else {
                        push(a);
                    }
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.MakeArray -> {
                    int count = readByte();
                    List<Value> array = new ArrayList<>();
                    for (int i = 0; i < count; i++)
                        array.add(pop());
                    push(new Value(array));
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.MakeMap -> {
                    int count = readByte();
                    Map<Value, Value> map = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        Value value = pop();
                        Value key = pop();
                        map.put(key, value);
                    }
                    push(new Value(map));
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Class -> {
                    String name = readString();
                    boolean hasSuper = readByte() == 1;
                    LanguageClass superClass = hasSuper ? pop().asClass() : null;

                    int attributeCount = readByte();
                    Map<String, ClassAttribute> attributes = new HashMap<>();
                    for (int i = 0; i < attributeCount; i++) {
                        String attrname = readString();
                        boolean isprivate = readByte() == 1;
                        boolean isstatic = readByte() == 1;
                        attributes.put(attrname, new ClassAttribute(pop(), isstatic, isprivate));
                    }

                    List<String> genericNames = new ArrayList<>();
                    int genericCount = readByte();
                    for (int i = 0; i < genericCount; i++)
                        genericNames.add(readString());


                    push(new Value(new LanguageClass(name, attributes, genericNames, superClass)));
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Method -> {
                    defineMethod(readString());
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.MakeVar -> {
                    int slot = readByte();
                    boolean constant = readByte() == 1;

                    Value at = get(slot);

                    stack.set(frame.slots + slot, new Value(new Var(at, constant)));
                    res = VirtualMachineResult.OK;
                }
                case ByteCodeOpCode.Access -> res = access();
                case ByteCodeOpCode.DropGlobal, ByteCodeOpCode.DropLocal, ByteCodeOpCode.DropUpvalue -> res = freeOps(instruction);
                case ByteCodeOpCode.Destruct -> res = destruct();
                case ByteCodeOpCode.Header -> res = header();
                default -> throw new RuntimeException("Unknown opcode: " + instruction);
            }

            if (res == VirtualMachineResult.EXIT) {
                return VirtualMachineResult.OK;
            } else if (res == VirtualMachineResult.ERROR) {
                if (frame.catchError) {
                    frames.pop();
                    if (frames.count == 0) {
                        return VirtualMachineResult.OK;
                    }

                    stack.setTop(frame.slots);
                    frame = frames.peek();
                    popTraceback();

                    Value result = new Value(new Result(lastError.getFirst(), lastError.getLast()));
                    push(result);
                    if (frame.optimization == 2) {
                        MEMO_CACHE.storeCache(result);
                    }

                    if (exitLevel == frames.count) {
                        return VirtualMachineResult.OK;
                    }
                    continue;
                }
                if (safe) {
                    while (frames.count > exitLevel) {
                        tracebacks.pop();
                        frames.pop();
                    }
                    frame = frames.peek();
                    stack.setTop(frames.peek(-1).slots);
                }
                return VirtualMachineResult.ERROR;
            }
        }
    }

    VirtualMachineResult destruct() {
        Namespace v = pop().asNamespace();
        push(new Value());
        int args = readByte();
        String[] names = new String[args];
        for (int i = 0; i < args; i++)
            names[i] = readString();
        Map<String, Var> values = v.values();
        for (String name : names) {
            if (values.containsKey(name)) {
                GLOBAL_VAR_ARGS.put(name, values.get(name));
            } else {
                runtimeError("Scope", "Undefined field: " + name);
                return VirtualMachineResult.ERROR;
            }
        }
        return VirtualMachineResult.OK;
    }

    VirtualMachineResult header() {
        int command = readByte();
        int argc = readByte();
        String[] args = new String[argc];
        for (int i = 0; i < argc; i++)
            args[i] = readString();

        int rArgc = switch (command) {
            case HeadCode.OPTIMIZE -> 0;
            case HeadCode.MAIN_CLASS, HeadCode.MAIN_FUNCTION -> 1;
            default -> -1;
        };
        if (argc != rArgc && rArgc != -1) {
            runtimeError("Argument Count", "Expected " + rArgc + " arguments, got " + argc);
            return VirtualMachineResult.ERROR;
        }

        switch (command) {
            case HeadCode.OPTIMIZE -> {
                if (frame.optimization == 0)
                    frame.optimization = 1;
            }
            case HeadCode.MAIN_FUNCTION -> mainFunction = args[0];
            case HeadCode.MAIN_CLASS -> mainClass = args[0];
            case HeadCode.EXPORT -> {
                if (exports == null) {
                    exports = new ArrayList<>();
                }
                exports.addAll(Arrays.asList(args));
            }
        }

        push(new Value());
        return VirtualMachineResult.OK;
    }

    public void finish(String[] args) {
        List<Value> argsV = new ArrayList<>();
        for (String arg : args) {
            argsV.add(new Value(arg));
        }
        Value val = new Value(argsV);

        if (mainFunction != null) {
            Var var = GLOBAL_VAR_ARGS.get(mainFunction);
            if (var == null || !var.val.isClosure) {
                runtimeError("Scope", "Main function not found");
            }

            assert var != null;
            Closure closure = var.val.asClosure();
            push(new Value(closure));

            push(val);

            boolean res = call(closure, new Value[]{val}, new HashMap<>());
            if (!res) {
                return;
            }
            run();
        } else if (mainClass != null) {
            Var var = GLOBAL_VAR_ARGS.get(mainClass);
            if (var == null || !var.val.isClass) {
                runtimeError("Scope", "Main class not found");
            }

            assert var != null;
            LanguageClass clazz = var.val.asClass();
            Value method = clazz.getField("main", true);
            if (method == null || !method.isClosure) {
                runtimeError("Scope", "Main method not found");
            }
            assert method != null;
            Closure closure = method.asClosure();

            push(new Value(closure));
            push(val);

            boolean res = call(closure, new Value(clazz), new Value[]{val}, new HashMap<>());
            if (!res) {
                return;
            }
            run();
        }
    }

    VirtualMachineResult iterator() {
        int iterated = readByte();
        int variable = readByte();
        int jump = readByte();

        List<Value> values = get(iterated).asVar().val.asList();
        if (values.size() == 0) {
            moveIP(jump);
            return VirtualMachineResult.OK;
        }

        get(variable).asVar().val(values.get(0));
        values.remove(0);
        return VirtualMachineResult.OK;
    }

    public Namespace asNamespace(String name) {
        return new Namespace(name, GLOBAL_VAR_ARGS, exports == null ? new ArrayList<>(GLOBAL_VAR_ARGS.keySet()) : exports);
    }

    public String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    public static VirtualMachine getInstance() {
        return instance;
    }
}
