package sandbox.java.util.concurrent.locks;

import sandbox.java.util.concurrent.TimeUnit;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.locks.Lock}
 * to allow us to compile {@link sandbox.java.util.concurrent.locks.ReentrantLock}.
 */
@SuppressWarnings("unused")
public interface Lock {
    void lock();

    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    void unlock();

    Condition newCondition();
}
