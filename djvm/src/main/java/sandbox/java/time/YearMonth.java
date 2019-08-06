package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.YearMonth}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class YearMonth extends sandbox.java.lang.Object implements Serializable {
    private final int year;
    private final int month;

    private YearMonth(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    @Override
    @NotNull
    protected java.time.YearMonth fromDJVM() {
        return java.time.YearMonth.of(year, month);
    }

    public static YearMonth of(int year, int month) {
        return new YearMonth(year, month);
    }
}
