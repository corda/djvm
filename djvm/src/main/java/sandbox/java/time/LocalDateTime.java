package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.LocalDateTime}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public class LocalDateTime extends sandbox.java.lang.Object implements Serializable {
    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    public LocalDate toLocalDate() {
        return date;
    }

    public LocalTime toLocalTime() {
        return time;
    }

    @Override
    @NotNull
    protected java.time.LocalDateTime fromDJVM() {
        return java.time.LocalDateTime.of(date.fromDJVM(), time.fromDJVM());
    }

    @NotNull
    public static LocalDateTime of(LocalDate date, LocalTime time) {
        return new LocalDateTime(date, time);
    }
}
