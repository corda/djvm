package sandbox.java.time;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.Year}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class Year extends sandbox.java.lang.Object implements Serializable {
    private final int year;

    private Year(int year) {
        this.year = year;
    }

    public int getValue() {
        return year;
    }

    @Override
    @NotNull
    protected java.time.Year fromDJVM() {
        return java.time.Year.of(year);
    }

    @NotNull
    public static Year of(int year) {
        return new Year(year);
    }
}
