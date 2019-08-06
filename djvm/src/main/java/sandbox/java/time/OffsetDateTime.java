package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.OffsetDateTime}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public class OffsetDateTime extends sandbox.java.lang.Object implements Serializable {
    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }

    @Override
    @NotNull
    protected java.time.OffsetDateTime fromDJVM() {
        return java.time.OffsetDateTime.of(dateTime.fromDJVM(), (java.time.ZoneOffset) offset.fromDJVM());
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset) {
        return new OffsetDateTime(dateTime, offset);
    }
}
