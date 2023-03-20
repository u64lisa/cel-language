package assembler.emulation;

import java.nio.ByteBuffer;

public class Memory {

    public final byte[] mem;
    private final ByteBuffer memWindow;

    public Memory(int sizeInBytes) {
        this.mem = new byte[sizeInBytes];
        this.memWindow = ByteBuffer.wrap(this.mem);
    }


    public int sizeInBytes() {
        return this.mem.length;
    }

    @SuppressWarnings("deprecation")
    public void storeStr(int address, String str) {
        str.getBytes(0, str.length(), this.mem, address);
        this.memWindow.put(address + str.length(), (byte) '\0');
    }

    public void storeBytes(int address, byte[] buf, int offset, int length) {
        System.arraycopy(buf, offset, this.mem, address, length);
    }

    public void storeInt(int address, int value) {
        this.memWindow.putInt(address, value);
    }

    public void storeFloat(int address, float value) {
        this.memWindow.putFloat(address, value);
    }

    public void storeByte(int address, byte value) {
        this.memWindow.put(address, value);
    }

    public void readBytes(int address, byte[] buf, int offset, int length) {
        System.arraycopy(this.mem, address, buf, offset, length);
    }

    public int readInt(int address) {
        return this.memWindow.getInt(address);
    }

    public float readFloat(int address) {
        return this.memWindow.getFloat(address);
    }

    public byte readByte(int address) {
        return this.memWindow.get(address);
    }
}
