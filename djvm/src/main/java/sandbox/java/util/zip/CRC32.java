package sandbox.java.util.zip;

import sandbox.java.nio.ByteBuffer;

@SuppressWarnings("unused")
public class CRC32 extends sandbox.java.lang.Object implements Checksum {

    private final java.util.zip.CRC32 crc;

    public CRC32() {
        crc = new java.util.zip.CRC32();
    }

    @Override
    public void update(int b) {
        crc.update(b);
    }

    @Override
    public void update(byte[] buffer, int offset, int length) {
        crc.update(buffer, offset, length);
    }

    public void update(byte[] buffer) {
        crc.update(buffer);
    }

    public void update(ByteBuffer buffer)  {
        int pos = buffer.position();
        int limit = buffer.limit();
        int remaining = limit - pos;
        if (remaining <= 0) {
            return;
        }

        if (buffer.hasArray()) {
            crc.update(buffer.array(), pos + buffer.arrayOffset(), remaining);
        } else {
            byte[] bytes = new byte[remaining];
            buffer.get(bytes);
            crc.update(bytes);
        }

        buffer.position(limit);
    }

    @Override
    public void reset() {
        crc.reset();
    }

    @Override
    public long getValue() {
        return crc.getValue();
    }
}
