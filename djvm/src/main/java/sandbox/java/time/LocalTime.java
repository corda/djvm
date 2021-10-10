package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.LocalTime}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class LocalTime extends sandbox.java.lang.Object implements Serializable {
    static final long NANOS_PER_SECOND = 1000_000_000L;

    private final byte hour;
    private final byte minute;
    private final byte second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nanoOfSecond) {
        this.hour = (byte) hour;
        this.minute = (byte) minute;
        this.second = (byte) second;
        this.nano = nanoOfSecond;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getNano() {
        return nano;
    }

    @Override
    @NotNull
    protected java.time.LocalTime fromDJVM() {
        return java.time.LocalTime.of(hour, minute, second, nano);
    }

    @NotNull
    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond) {
        return new LocalTime(hour, minute, second, nanoOfSecond);
    }
}
