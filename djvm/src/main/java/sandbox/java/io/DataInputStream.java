package sandbox.java.io;

import sandbox.java.lang.String;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This is a dummy class that implements just enough of {@link java.io.DataInputStream}
 * to allow us to compile {@link sandbox.java.util.Currency}.
 */
@SuppressWarnings("unused")
public class DataInputStream extends InputStream {
    private static final java.lang.String NOT_IMPLEMENTED = "Dummy class - not implemented";

    public DataInputStream(InputStream input) {
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedEncodingException(NOT_IMPLEMENTED);
    }

    public int readInt() throws IOException {
        throw new UnsupportedEncodingException(NOT_IMPLEMENTED);
    }

    public long readLong() throws IOException {
        throw new UnsupportedEncodingException(NOT_IMPLEMENTED);
    }

    public String readUTF() throws IOException {
        throw new UnsupportedEncodingException(NOT_IMPLEMENTED);
    }
}
