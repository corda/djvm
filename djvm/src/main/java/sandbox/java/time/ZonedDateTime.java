package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.ZonedDateTime}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class ZonedDateTime extends sandbox.java.lang.Object implements Serializable {
    private final LocalDateTime dateTime;
    private final ZoneOffset offset;
    private final ZoneId zone;

    private ZonedDateTime(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public ZoneId getZone() {
        return zone;
    }

    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }

    private static ZonedDateTime ofLenient(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        return new ZonedDateTime(localDateTime, offset, zone);
    }

    @Override
    @NotNull
    protected java.time.ZonedDateTime fromDJVM() {
        return DJVM.zonedDateTime(dateTime, offset, zone);
    }

    @NotNull
    public static ZonedDateTime createDJVM(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        return ofLenient(localDateTime, offset, zone);
    }
}
