package language.backend.compiler.bytecode.headers;

import language.backend.compiler.bytecode.values.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MemoCache {
    private List<Cache> caches;
    private Stack<Cache> stack;

    public MemoCache() {
        this.caches = new ArrayList<>();
        this.stack = new Stack<>();
    }

    public void stackCache(String name, Value[] args) {
        Cache cache = new Cache(name, args, null);
        caches.add(cache);
        stack.add(cache);
    }

    public void storeCache(Value result) {
        stack.pop().store(result);
    }

    public Value get(String key, Value[] args) {
        for (Cache cache : caches) {
            if (cache.equals(key, args)) {
                return cache.getValue();
            }
        }
        return null;
    }

    public List<Cache> getCaches() {
        return caches;
    }

    public void setCaches(List<Cache> caches) {
        this.caches = caches;
    }

    public Stack<Cache> getStack() {
        return stack;
    }

    public void setStack(Stack<Cache> stack) {
        this.stack = stack;
    }
}
