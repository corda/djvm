package sandbox.java.util;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.util.UUID}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class UUID extends sandbox.java.lang.Object implements Serializable {
    private final long mostSigBits;
    private final long leastSigBits;

    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    public long getLeastSignificantBits() {
        return leastSigBits;
    }

    public long getMostSignificantBits() {
        return mostSigBits;
    }

    @Override
    @NotNull
    protected java.util.UUID fromDJVM() {
        return new java.util.UUID(mostSigBits, leastSigBits);
    }
}
