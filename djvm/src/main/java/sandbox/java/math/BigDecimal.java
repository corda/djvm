package sandbox.java.math;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.Comparable;
import sandbox.java.lang.Number;

/**
 * This is a dummy class that implements just enough of {@link java.math.BigDecimal}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BigDecimal extends Number implements Comparable<BigDecimal> {
    private static final java.lang.String UNSUPPORTED = "Dummy class - not implemented";

    public BigDecimal(BigInteger unscaledVal, int scale) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public int compareTo(@NotNull BigDecimal bigDecimal) {
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

    public BigInteger unscaledValue() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    public int scale() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    /**
     * This method will actually be "stitched" into {@link BigDecimal} at runtime.
     * It has been implemented here mainly for reference.
     * @return An equivalent {@link java.math.BigDecimal} object.
     */
    @Override
    @NotNull
    protected final java.math.BigDecimal fromDJVM() {
        return new java.math.BigDecimal(unscaledValue().fromDJVM(), scale());
    }
}
