package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.MonthDay}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class MonthDay extends sandbox.java.lang.Object implements Serializable {
    private final int month;
    private final int day;

    private MonthDay(int month, int day) {
        this.month = month;
        this.day = day;
    }

    public int getMonthValue() {
        return month;
    }

    public int getDayOfMonth() {
        return day;
    }

    @Override
    @NotNull
    protected java.time.MonthDay fromDJVM() {
        return java.time.MonthDay.of(month, day);
    }

    @NotNull
    public static MonthDay of(int month, int dayOfMonth) {
        return new MonthDay(month, dayOfMonth);
    }
}
