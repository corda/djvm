package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static sandbox.java.time.LocalTime.NANOS_PER_SECOND;

/**
 * This is a dummy class that implements just enough of {@link java.time.Duration}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class Duration extends sandbox.java.lang.Object implements Serializable {
    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    public long getSeconds() {
        return seconds;
    }

    public int getNano() {
        return nanos;
    }

    @Override
    @NotNull
    protected java.time.Duration fromDJVM() {
        return java.time.Duration.ofSeconds(seconds, nanos);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        int nanos = (int)Math.floorMod(nanoAdjustment, NANOS_PER_SECOND);
        return new Duration(seconds, nanos);
    }
}
