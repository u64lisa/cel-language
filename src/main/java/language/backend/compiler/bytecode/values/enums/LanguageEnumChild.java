package language.backend.compiler.bytecode.values.enums;

import language.backend.compiler.bytecode.ChunkCode;
import language.backend.compiler.bytecode.values.Value;
import language.backend.compiler.bytecode.values.classes.ClassAttribute;
import language.backend.compiler.bytecode.values.classes.Instance;
import language.vm.VirtualMachine;

import java.util.*;

public class LanguageEnumChild {
    final int value;
    LanguageEnum parent;

    private final Value asValue;

    // For Enum Props
    public final List<String> props;

    public final int arity;

    public LanguageEnumChild(int value, List<String> props) {
        this.value = value;
        this.props = props;

        this.arity = props.size();

        this.asValue = new Value(this);
    }

    public int getValue() {
        return value;
    }

    public boolean equals(LanguageEnumChild other) {
        return value == other.value;
    }

    public void setParent(LanguageEnum languageEnum) {
        parent = languageEnum;
    }

    public String type() {
        return parent.name();
    }

    public LanguageEnum getParent() {
        return parent;
    }

    public Value create(Value[] args, VirtualMachine virtualMachine) {
        Map<String, ClassAttribute> fields = new HashMap<>();
        for (int i = 0; i < props.size(); i++) {
            fields.put(
                    props.get(i),
                    new ClassAttribute(args[i])
            );
        }

        fields.put("$child", new ClassAttribute(new Value(value)));
        fields.put("$parent", new ClassAttribute(new Value(parent)));

        // Normal: EnumChild
        // Generic: EnumChild((Type1)(Type2)(etc))
        return new Value(new Instance(parent.name(), fields, virtualMachine));
    }

    public Value asValue() {
        return asValue;
    }

    public int[] dump() {
        List<Integer> dump = new ArrayList<>(Arrays.asList(ChunkCode.EnumChild, value));
        dump.add(props.size());
        for (String prop : props) {
            Value.addAllString(dump, prop);
        }
        return dump.stream().mapToInt(Integer::intValue).toArray();
    }
}
