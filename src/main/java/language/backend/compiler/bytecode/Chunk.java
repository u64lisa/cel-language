package language.backend.compiler.bytecode;

import language.backend.compiler.bytecode.types.Type;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.ValueArray;

import java.util.*;

public class Chunk {
    List<Integer> code;
    public int[] codeArray;
    public String packageName;
    public String target;
    ValueArray constants;
    final String source;
    public Map<String, Type> globals;

    public Chunk(String source) {
        this.code = new ArrayList<>();
        this.constants = new ValueArray();
        this.source = source;
        this.globals = new HashMap<>();
    }

    public void write(int b) {
        code.add(b);
    }

    public void compile() {
        codeArray = new int[code.size()];
        for (int index = 0; index < code.size(); index++) {
            codeArray[index] = code.get(index);
        }
        constants.compile();
    }

    public int addConstant(Value value) {
        return constants.write(value);
    }

    public String source() {
        return source;
    }

    public ValueArray constants() {
        return constants;
    }
    public void constants(ValueArray constants) {
        this.constants = constants;
    }

    public int[] dump() {
        List<Integer> list = new ArrayList<>(Collections.singletonList(ChunkCode.Chunk));
        Value.addAllString(list, source);
        if (packageName != null) {
            Value.addAllString(list, packageName);
        }
        else {
            list.add(0);
        }
        if (target != null) {
            Value.addAllString(list, target);
        }
        else {
            list.add(0);
        }
        for (int index : constants().dump())
            list.add(index);
        list.add(globals.size());
        for (Map.Entry<String, Type> entry : globals.entrySet()) {
            Value.addAllString(list, entry.getKey());
            list.addAll(entry.getValue().dumpList());
        }
        list.add(codeArray.length);
        for (int index : codeArray)
            list.add(index);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

}
