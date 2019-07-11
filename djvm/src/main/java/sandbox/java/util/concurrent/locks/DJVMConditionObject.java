package sandbox.java.util.concurrent.locks;

import sandbox.java.util.Date;
import sandbox.java.util.concurrent.TimeUnit;

class DJVMConditionObject extends sandbox.java.lang.Object implements Condition {
    DJVMConditionObject() {
    }

    @Override
    public void await() {
    }

    @Override
    public void awaitUninterruptibly() {
    }

    @Override
    public long awaitNanos(long nanosTimeout) {
        return 0;
    }

    @Override
    public boolean await(long time, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean awaitUntil(Date deadline) {
        return true;
    }

    @Override
    public void signal() {
    }

    @Override
    public void signalAll() {
    }
}
