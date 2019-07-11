package sandbox.java.util.concurrent.locks;

import sandbox.java.util.Date;
import sandbox.java.util.concurrent.TimeUnit;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.locks.Condition}
 * to allow us to compile {@link sandbox.java.util.concurrent.locks.DJVMConditionObject}.
 */
@SuppressWarnings("unused")
public interface Condition {
    void await() throws InterruptedException;

    void awaitUninterruptibly();

    long awaitNanos(long nanosTimeout) throws InterruptedException;

    boolean await(long time, TimeUnit unit) throws InterruptedException;

    boolean awaitUntil(Date deadline) throws InterruptedException;

    void signal();

    void signalAll();
}
