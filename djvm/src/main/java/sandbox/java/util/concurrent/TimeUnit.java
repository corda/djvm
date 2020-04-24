package sandbox.java.util.concurrent;

import sandbox.java.lang.Enum;
import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.TimeUnit}
 * to allow us to compile {@link sandbox.java.util.concurrent.locks.ReentrantLock}.
 */
public abstract class TimeUnit extends Enum<TimeUnit> {
    private TimeUnit(String name, int ordinal) {
        super(name, ordinal);
    }
}
