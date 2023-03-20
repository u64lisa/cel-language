package language.backend.compiler.bytecode.values.enums;

import language.backend.compiler.ChunkCode;
import language.backend.compiler.bytecode.values.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LanguageEnum {
    String name;
    Map<String, LanguageEnumChild> children;

    public LanguageEnum(String name, Map<String, LanguageEnumChild> children) {
        this.name = name;
        this.children = children;

        for (LanguageEnumChild child : children.values()) {
            child.setParent(this);
        }
    }

    public String name() {
        return name;
    }

    public Value get(String name) {
        return children.get(name).asValue();
    }

    public int[] dump() {
        List<Integer> list = new ArrayList<>(Collections.singletonList(ChunkCode.Enum));
        for (int i : Value.dumpString(name)) {
            list.add(i);
        }
        list.add(children.size());
        for (Map.Entry<String, LanguageEnumChild> entry : children.entrySet()) {
            for (int i : Value.dumpString(entry.getKey())) {
                list.add(i);
            }
            for (int i : entry.getValue().dump()) {
                list.add(i);
            }
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public Map<String, LanguageEnumChild> children() {
        return children;
    }
}
