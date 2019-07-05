package sandbox.java.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * This is a dummy class that implements just enough of {@link java.io.InputStream}
 * to allow us to compile {@link DJVMInputStream}.
 */
@SuppressWarnings("unused")
public abstract class InputStream extends sandbox.java.lang.Object implements Closeable {
    private static final String NOT_IMPLEMENTED = "Dummy class - not implemented";

    public abstract int read() throws IOException;

    public int read(byte[] buffer) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public int read(byte[] buffer, int offs4t, int length) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public int available() throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public long skip(long length) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public void close() throws IOException {
    }

    public boolean markSupported() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public void mark(int readLimit) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * This method will actually be "stitched" into {@link InputStream} at runtime.
     * It has been implemented here mainly for reference.
     */
    public static InputStream toDJVM(java.io.InputStream input) {
        return (input == null) ? null : new DJVMInputStream(input);
    }
}
