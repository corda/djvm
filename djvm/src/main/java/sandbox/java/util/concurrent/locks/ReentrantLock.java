package sandbox.java.util.concurrent.locks;

import sandbox.java.lang.Thread;
import sandbox.java.util.ArrayList;
import sandbox.java.util.Collection;
import sandbox.java.util.concurrent.TimeUnit;

import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ReentrantLock extends sandbox.java.lang.Object implements Lock, Serializable {
    private final boolean isFair;
    private int holdCount;

    public ReentrantLock(boolean isFair) {
        this.isFair = isFair;
    }

    public ReentrantLock() {
        this(false);
    }

    @Override
    public void lock() {
        ++holdCount;
    }

    @Override
    public void lockInterruptibly() {
        lock();
    }

    @Override
    public boolean tryLock() {
        lock();
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        return tryLock();
    }

    @Override
    public void unlock() {
        if (holdCount > 0) {
            --holdCount;
        }
    }

    @Override
    public Condition newCondition() {
        return new DJVMConditionObject();
    }

    public int getHoldCount() {
        return holdCount;
    }

    public boolean isLocked() {
        return holdCount > 0;
    }

    public boolean isHeldByCurrentThread() {
        return isLocked();
    }

    public final boolean isFair() {
        return isFair;
    }

    protected Thread getOwner() {
        return null;
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    public final boolean hasQueuedThread(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        return false;
    }

    public final int getQueueLength() {
        return 0;
    }

    protected Collection<Thread> getQueuedThreads() {
        return new ArrayList<>();
    }

    public boolean hasWaiters(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        return false;
    }

    public int getWaitQueueLength(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        return 0;
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        return new ArrayList<>();
    }
}
