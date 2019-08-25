package sandbox.java.util;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.Comparable;

/**
 * This is a dummy class that implements just enough of {@link java.util.Date}
 * to allow us to compile {@link sandbox.java.util.concurrent.locks.ReentrantLock}.
 */
public class Date extends sandbox.java.lang.Object implements java.io.Serializable, Cloneable, Comparable<Date> {
    private static final String NOT_IMPLEMENTED = "Dummy class - not implemented";

    private final long time;

    public Date(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    @Override
    public int compareTo(@NotNull Date o) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    @NotNull
    protected final java.util.Date fromDJVM() {
        return new java.util.Date(getTime());
    }
}
