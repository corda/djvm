package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.Period}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class Period extends sandbox.java.lang.Object implements Serializable {
    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    @Override
    @NotNull
    protected java.time.Period fromDJVM() {
        return java.time.Period.of(years, months, days);
    }

    @NotNull
    public static Period of(int years, int months, int days) {
        return new Period(years, months, days);
    }
}
