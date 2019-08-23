package sandbox.java.nio;

/**
 * This is a dummy class that implements just enough of {@link java.nio.ByteOrder}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
public final class ByteOrder extends sandbox.java.lang.Object {

    public static final ByteOrder BIG_ENDIAN = new ByteOrder();
    public static final ByteOrder LITTLE_ENDIAN = new ByteOrder();
}
