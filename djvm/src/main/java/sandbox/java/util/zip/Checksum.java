package sandbox.java.util.zip;

/**
 * This is a dummy class that implements just enough of {@link java.util.zip.Checksum}
 * to allow us to compile {@link sandbox.java.util.zip.CRC32}.
 */
@SuppressWarnings("unused")
public interface Checksum {

    void update(int b);

    void update(byte[] buffer, int offset, int length);

    long getValue();

    void reset();

}
