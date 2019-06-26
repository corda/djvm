package sandbox.java.io;

import java.io.IOException;

/**
 * This is a dummy class that implements just enough of {@link java.io.BufferedInputStream}
 * to allow us to compile {@link sandbox.java.util.Currency}.
 */
@SuppressWarnings({"unused", "RedundantThrows"})
public class BufferedInputStream extends InputStream {
    public BufferedInputStream(InputStream input) {
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }
}
