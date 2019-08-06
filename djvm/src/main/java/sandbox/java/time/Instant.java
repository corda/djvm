package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static sandbox.java.time.LocalTime.NANOS_PER_SECOND;

/**
 * This is a dummy class that implements just enough of {@link java.time.Instant}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class Instant extends sandbox.java.lang.Object implements Serializable {
    private final long seconds;
    private final int nanos;

    private Instant(long epochSecond, int nano) {
        this.seconds = epochSecond;
        this.nanos = nano;
    }

    public long getEpochSecond() {
        return seconds;
    }

    public int getNano() {
        return nanos;
    }

    @Override
    @NotNull
    protected java.time.Instant fromDJVM() {
        return java.time.Instant.ofEpochSecond(seconds, nanos);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        int nanos = (int)Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return new Instant(epochSecond, nanos);
    }
}
