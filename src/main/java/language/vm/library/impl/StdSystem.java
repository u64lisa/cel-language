package language.vm.library.impl;

import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;

@LibraryClass(className = "System")
public class StdSystem {

    private static final boolean DEBUG = false;

    @LibraryMethod
    public void println(final Object message) {
        System.out.println(message);
    }

    @LibraryMethod
    public void print(final Object message) {
        System.out.print(message);
    }

    @LibraryMethod
    public void err(final Object message) {
        System.err.println(message);
    }

    @LibraryMethod
    public long epoch() {
        return System.currentTimeMillis();
    }

    @LibraryMethod
    public String str(final Object value) {
        return value.toString();
    }

    @LibraryMethod
    public void exit(final int value) {
        System.exit(value);
    }

}
