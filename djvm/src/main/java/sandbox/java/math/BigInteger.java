package sandbox.java.math;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.Comparable;
import sandbox.java.lang.Number;

/**
 * This is a dummy class that implements just enough of {@link java.math.BigInteger}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BigInteger extends Number implements Comparable<BigInteger> {
    private static final java.lang.String UNSUPPORTED = "Dummy class - not implemented";

    public BigInteger(int signum, byte[] magnitude) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public int compareTo(@NotNull BigInteger bigInteger) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public double doubleValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public float floatValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public long longValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public int intValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public short shortValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public byte byteValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public int signum() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public byte[] toByteArray() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    /**
     * This method will actually be "stitched" into {@link BigInteger} at runtime.
     * It has been implemented here mainly for reference.
     * @return An equivalent {@link java.math.BigInteger} object.
     */
    @Override
    @NotNull
    protected final java.math.BigInteger fromDJVM() {
        return new java.math.BigInteger(signum(), toByteArray());
    }
}
