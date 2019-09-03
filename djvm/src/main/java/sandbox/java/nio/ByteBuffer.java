package sandbox.java.nio;

import sandbox.java.lang.Comparable;

/**
 * This is a dummy class that implements just enough of {@link java.nio.ByteBuffer}
 * to allow us to compile {@link sandbox.java.util.zip.CRC32}.
 */
public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {
    private static final String UNSUPPORTED = "Dummy class - not implemented";

    public final boolean hasArray() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public final int arrayOffset() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public final byte[] array() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public ByteBuffer get(byte[] target) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }
}
