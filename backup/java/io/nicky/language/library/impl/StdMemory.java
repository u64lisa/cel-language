package language.vm.library.impl;

import language.vm.library.LibraryClass;
import language.vm.library.LibraryMethod;
import language.vm.library.impl.memory.IMemory;
import language.vm.library.impl.memory.Memory;

import java.nio.charset.StandardCharsets;

@LibraryClass(className = "Memory")
public class StdMemory {

    private final IMemory memory = Memory.memory();

    @LibraryMethod
    public long MEM_store_string_to_bytes(final long address, final Object any) {
        final String text = any.toString();
        final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        this.memory.put(address, bytes);
        return address;
    }

    @LibraryMethod
    public long MEM_alloc(int size) {
        return memory.alloc(size);
    }

    @LibraryMethod
    public long MEM_reallocate(long address, int size) {
        return memory.reallocate(address, size);
    }

    @LibraryMethod
    public void MEM_free(long address) {
        memory.free(address);
    }

    @LibraryMethod
    public void MEM_copy(long first, long last) {
        memory.copy(first, last);
    }

    @LibraryMethod
    public int MEM_getInt(long address) {
        return memory.getInt(address);
    }

    @LibraryMethod
    public void MEM_putInt(final long address, int value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public void MEM_putFloat(long address, float value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public float MEM_getFloat(long address) {
        return memory.getFloat(address);
    }

    @LibraryMethod
    public void MEM_putLong(long address, long value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public float MEM_getLong(long address) {
        return memory.getLong(address);
    }

    @LibraryMethod
    public void MEM_putDouble(long address, double value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public double MEM_getDouble(long address) {
        return memory.getDouble(address);
    }

    @LibraryMethod
    public void MEM_putChar(long address, char value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public char MEM_getChar(long address) {
        return memory.getChar(address);
    }

    @LibraryMethod
    public void MEM_putShort(long address, short value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public short MEM_getShort(long address) {
        return memory.getShort(address);
    }

    @LibraryMethod
    public void MEM_putByte(long address, byte value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public byte MEM_getByte(long address) {
        return memory.getByte(address);
    }

    @LibraryMethod
    public void pMEM_utBoolean(long address, boolean value) {
        memory.put(address, value);
    }

    @LibraryMethod
    public boolean MEM_getBoolean(long address) {
        return memory.getBoolean(address);
    }

    @LibraryMethod
    public void MEM_putByteArray(long address, byte[] data) {
        memory.put(address, data);
    }

    public IMemory getMemory() {
        return memory;
    }
}
