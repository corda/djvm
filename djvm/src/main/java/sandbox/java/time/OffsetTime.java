package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.OffsetTime}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class OffsetTime extends sandbox.java.lang.Object implements Serializable {
    private final LocalTime time;
    private final ZoneOffset offset;

    private OffsetTime(LocalTime time, ZoneOffset offset) {
        this.time = time;
        this.offset = offset;
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public LocalTime toLocalTime() {
        return time;
    }

    @Override
    @NotNull
    protected java.time.OffsetTime fromDJVM() {
        return java.time.OffsetTime.of(time.fromDJVM(), (java.time.ZoneOffset) offset.fromDJVM());
    }

    @NotNull
    public static OffsetTime of(LocalTime time, ZoneOffset offset) {
        return new OffsetTime(time, offset);
    }
}
