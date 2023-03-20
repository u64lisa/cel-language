package language.vm.library.impl.memory;

public interface IMemory {
    long alloc(Class<?> type);

    long alloc(int size);

    long reallocate(long address, int size);

    void free(long address);

    void copy(long first, long last);

    int getInt(long address);

    void put(long address, float value);

    float getFloat(long address);

    void put(long address, long value);

    float getLong(long address);

    void put(long address, double value);

    double getDouble(long address);

    void put(long address, char value);

    char getChar(long address);

    void put(long address, short value);

    short getShort(long address);

    void put(long address, byte value);

    byte getByte(long address);

    void put(long address, boolean value);

    boolean getBoolean(long address);

    void put(long address, byte[] data);

    byte[] getByteArray(long address);

    int typeSize(Class<?> type);

    int typeSize(String type);
}
