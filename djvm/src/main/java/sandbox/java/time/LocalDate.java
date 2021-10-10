package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.LocalDate}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class LocalDate extends sandbox.java.lang.Object implements Serializable {
    private final int year;
    private final short month;
    private final short day;

    private LocalDate(int year, int month, int dayOfMonth) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) dayOfMonth;
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    public int getDayOfMonth() {
        return day;
    }

    @Override
    @NotNull
    protected java.time.LocalDate fromDJVM() {
        return java.time.LocalDate.of(year, month, day);
    }

    @NotNull
    public static LocalDate of(int year, int month, int dayOfMonth) {
        return new LocalDate(year, month, dayOfMonth);
    }
}
