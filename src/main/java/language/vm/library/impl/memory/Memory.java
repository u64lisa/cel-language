package language.vm.library.impl.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public final class Memory implements IMemory {

    private static final IMemory INTERNAL = new Memory();

    private static final Random RANDOM = ThreadLocalRandom.current();

    private record MemorySegment(long address, byte[] data) {

        public int toInt() {
            return bytesToInt(data);
        }

        public long toLong() {
            return bytesToLong(data);
        }

        public short toShort() {
            return bytesToShort(data);
        }

        public double toDouble() {
            return bytesToLong(data);
        }

        public float toFloat() {
            return bytesToLong(data);
        }

        public boolean toBoolean() {
            return data[0] == 1;
        }

        public char toChar() {
            return bytesToChar(data);
        }

        public byte toByte() {
            return data[0];
        }

        public byte[] toArray() {
            return data;
        }

    }

    private Memory() {}

    public static IMemory memory() {
        return INTERNAL;
    }

    private static class MemoryException extends RuntimeException {
        public MemoryException(String message) {
            super(message);
        }
    }

    private final List<MemorySegment> segments = new ArrayList<>();

    @Override
    public long alloc(final Class<?> type) {
        return alloc(this.typeSize(type));
    }

    @Override
    public long alloc(final int size) {
        long address = RANDOM.nextLong(0xFFFFFF);
        this.segments.add(new MemorySegment(address, new byte[size]));
        return address;
    }

    @Override
    public long reallocate(final long address, int size) {
        this.free(address);
        return alloc(size);
    }

    @Override
    public void free(final long address) {
        segments.removeIf(memorySegment -> memorySegment.address == address);
    }

    @Override
    public void copy(final long first, final long last) {
        final MemorySegment firstSegment = get0(first);
        final MemorySegment lastSegment = get0(last);


        if (firstSegment.data.length != lastSegment.data.length)
            throw new MemoryException("Invalid stack size for allocated memory");

        for (int index = 0; index < firstSegment.data.length; index++) {
            byte current = firstSegment.data[index];
            lastSegment.data[index] = current;
        }
    }

    public void put(final long address, int value) {
        this.put(address, intToBytes(value));
    }

    public void putByte(final long address, final byte single) {
        this.put(address, new byte[]{single});
    }

    private MemorySegment get0(final long address) {
        final MemorySegment memorySegment = this.segments
                .stream()
                .filter(segment -> segment.address == address)
                .findAny()
                .orElse(null);

        if (memorySegment == null)
            throw new MemoryException("No register found for address: " + address);

        return memorySegment;
    }

    @Override
    public int getInt(final long address) {
        return this.get0(address).toInt();
    }

    @Override
    public void put(final long address, float value) {
        this.put(address, longToBytes((long) value));
    }

    @Override
    public float getFloat(final long address) {
        return this.get0(address).toFloat();
    }

    @Override
    public void put(final long address, long value) {
        this.put(address, longToBytes(value));
    }

    @Override
    public float getLong(final long address) {
        return this.get0(address).toLong();
    }

    @Override
    public void put(final long address, double value) {
        this.put(address, longToBytes((long) value));
    }

    @Override
    public double getDouble(final long address) {
        return this.get0(address).toDouble();
    }

    @Override
    public void put(final long address, char value) {
        this.put(address, charToBytes(value));
    }

    @Override
    public char getChar(final long address) {
        return this.get0(address).toChar();
    }

    @Override
    public void put(final long address, short value) {
        this.put(address, shortToBytes(value));
    }

    @Override
    public short getShort(final long address) {
        return this.get0(address).toShort();
    }

    @Override
    public void put(final long address, byte value) {
        this.put(address, new byte[value]);
    }

    @Override
    public byte getByte(final long address) {
        return this.get0(address).toByte();
    }

    @Override
    public void put(final long address, boolean value) {
        this.putByte(address, (byte) (value ? 1 : 0));
    }

    @Override
    public boolean getBoolean(final long address) {
        return this.get0(address).toBoolean();
    }

    @Override
    public void put(final long address, final byte[] data) {
        final MemorySegment segment = segments
                .stream()
                .filter(memorySegment -> memorySegment.address == address)
                .findAny()
                .orElse(null);

        if (segment == null)
            throw new MemoryException("No register found for address: " + address);

        if (segment.data.length != data.length)
            throw new MemoryException("Invalid stack size for allocated memory");

        for (int index = 0; index < data.length; index++) {
            byte current = data[index];
            segment.data[index] = current;
        }
    }

    @Override
    public byte[] getByteArray(long address) {
        return get0(address).toArray();
    }

    @Override
    public int typeSize(final Class<?> type) {
        if (type.equals(int.class)) // done
            return 8;
        if (type.equals(float.class) || type.equals(long.class) || type.equals(double.class)) // done
            return Long.BYTES;
        if (type.equals(short.class) || type.equals(char.class)) // done
            return Short.BYTES;
        if (type.equals(byte.class)) // done
            return Byte.BYTES;
        if (type.equals(boolean.class)) // done
            return 1; // 0 | 1

        return -1;
    }

    @Override
    public int typeSize(final String type) {
        if (type.equals("int")) // done
            return 8;
        if (type.equals("float") || type.equals("long") || type.equals("double")) // done
            return Long.BYTES;
        if (type.equals("short") || type.equals("char")) // done
            return Short.BYTES;
        if (type.equals("byte")) // done
            return Byte.BYTES;
        if (type.equals("boolean")) // done
            return 1; // 0 | 1

        return -1;
    }

    public static byte[] shortToBytes(short s) {
        return new byte[]{
                (byte) (s >>> 8),
                (byte) (s)
        };
    }

    public static short bytesToShort(final byte[] bytes) {
        return (short) ((bytes[0] & 0xFF) << 8 |
                ((bytes[1] & 0xFF)));
    }

    public static byte[] charToBytes(char s) {
        return new byte[]{
                (byte) (s >>> 8),
                (byte) (s)
        };
    }

    public static char bytesToChar(final byte[] bytes) {
        return (char) ((bytes[0] & 0xFF) << 8 |
                ((bytes[1] & 0xFF)));
    }

    public static byte[] intToBytes(int value) {
        return longToBytes(value);
    }

    public static int bytesToInt(byte[] bytes) {
        return (int) bytesToLong(bytes);
    }

    public static byte[] longToBytes(long longValue) {
        byte[] result = new byte[Long.BYTES];
        for (int index = Long.BYTES - 1; index >= 0; index--) {
            result[index] = (byte) (longValue & 0xFF);
            longValue >>= Byte.SIZE;
        }

        return result;
    }

    public static long bytesToLong(final byte[] bytes) {
        long result = 0;
        for (int index = 0; index < Long.BYTES; index++) {
            result <<= Byte.SIZE;
            result |= (bytes[index] & 0xFF);
        }
        return result;
    }

}

