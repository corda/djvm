package sandbox.java.nio;

/**
 * This is a dummy class that implements just enough of {@link java.nio.Buffer}
 * to allow us to compile {@link sandbox.java.util.zip.CRC32}.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class Buffer extends sandbox.java.lang.Object {
    private static final String UNSUPPORTED = "Dummy class - not implemented";

    public final int position() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public final Buffer position(int newPosition) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public final int limit() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }
}
