package language.backend.compiler.bytecode.values.bytecode;

import language.backend.compiler.bytecode.Chunk;
import language.backend.compiler.bytecode.ChunkCode;
import language.backend.compiler.bytecode.values.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteCode {
    public static final int ASYNC = 0x0001;
    public static final int CATCHER = 0x0010;
    public static final int VARARGS = 0x0100;
    public static final int KW_ARGS = 0x1000;

    public int arity;
    public int totarity;
    public Chunk chunk;
    public String name;

    public boolean catcher;

    // Only if the function is a method
    public boolean isPrivate;
    public boolean isStatic;
    public boolean isBin;
    public String owner;

    public int upvalueCount;
    public boolean async;
    public List<Value> defaults;
    public int defaultCount;
    public boolean varargs;
    public boolean kwargs;

    public ByteCode(String source) {
        arity = 0;
        totarity = 0;
        defaultCount = 0;
        name = "";
        chunk = new Chunk(source);

        upvalueCount = 0;
    }

    public String toString() {
        if (owner != null)
            return "<" + owner + "-method-" + name + ">";
        return "<function-" + name + ">";
    }

    public ByteCode copy() {
        ByteCode copy = new ByteCode(chunk.source());

        copy.chunk.constants(chunk.constants().copy());
        copy.chunk.codeArray = chunk.codeArray;

        copy.arity = arity;
        copy.defaults = defaults;
        copy.totarity = totarity;
        copy.name = name;
        copy.isPrivate = isPrivate;
        copy.isStatic = isStatic;
        copy.isBin = isBin;
        copy.owner = owner;
        copy.upvalueCount = upvalueCount;
        copy.async = async;
        copy.catcher = catcher;
        copy.kwargs = kwargs;
        copy.varargs = varargs;
        return copy;
    }

    public int[] dump() {
        List<Integer> list = new ArrayList<>(Arrays.asList(ChunkCode.Func, arity, totarity));
        if (name != null)
            Value.addAllString(list, name);
        else
            list.add(0);

        list.add(upvalueCount);

        int operation = 0;
        if (async)
            operation += ASYNC;
        if (catcher)
            operation += CATCHER;
        if (varargs)
            operation += VARARGS;
        if (kwargs)
            operation += KW_ARGS;

        list.add(operation);

        int[] chunk = this.chunk.dump();
        for (int i : chunk)
            list.add(i);

        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public byte[] dumpBytes() {
        int[] dump = dump();
        byte[] bytes = new byte[dump.length * 4];
        for (int i = 0; i < dump.length; i++) {
            int v = dump[i];
            bytes[i * 4] = (byte) (v >>> 24);
            bytes[i * 4 + 1] = (byte) (v >>> 16);
            bytes[i * 4 + 2] = (byte) (v >>> 8);
            bytes[i * 4 + 3] = (byte) (v);
        }
        return bytes;
    }
}
