package language.backend.compiler.bytecode.headers;

import language.backend.compiler.bytecode.values.Value;

public class Cache {

    private String name;
    private Value[] args;
    private Value result;

    public Cache(String name, Value[] args, Value result) {
        this.name = name;
        this.args = args;
        this.result = result;
    }

    public boolean equals(Object o) {
        if (o instanceof Cache) {
            Cache cache = (Cache) o;
            if (!cache.name.equals(name)) return false;
            if (cache.args.length != args.length) return false;
            for (int i = 0; i < args.length; i++) {
                if (!cache.args[i].equals(args[i])) return false;
            }
            return true;
        }
        return false;
    }

    public boolean equals(String name, Value[] args) {
        if (!this.name.equals(name)) return false;
        if (this.args.length != args.length) return false;
        for (int index = 0; index < args.length; index++) {
            if (!this.args[index].equals(args[index])) return false;
        }
        return true;
    }

    public Value getValue() {
        return result;
    }

    public void store(Value result) {
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value[] getArgs() {
        return args;
    }

    public void setArgs(Value[] args) {
        this.args = args;
    }

    public Value getResult() {
        return result;
    }

    public void setResult(Value result) {
        this.result = result;
    }
}
