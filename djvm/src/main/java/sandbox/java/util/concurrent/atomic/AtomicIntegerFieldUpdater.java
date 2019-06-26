package sandbox.java.util.concurrent.atomic;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater}
 * to allow us to compile {@link AtomicIntegerFieldUpdaterImpl}.
 */
@SuppressWarnings("unused")
public abstract class AtomicIntegerFieldUpdater<T> extends sandbox.java.lang.Object {
    public abstract boolean compareAndSet(T obj, int expect, int update);

    public abstract boolean weakCompareAndSet(T obj, int expect, int update);

    public abstract void set(T obj, int newValue);

    public abstract void lazySet(T obj, int newValue);

    public abstract int get(T obj);

    /**
     * Wrap the native {@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater} field inside
     * this because we cannot replicate {@link sun.reflect.Reflection#getCallerClass()} inside the
     * sandbox.
     */
    static final class AtomicIntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
        @SuppressWarnings("AtomicFieldUpdaterNotStaticFinal")
        private final java.util.concurrent.atomic.AtomicIntegerFieldUpdater<T> updater;

        AtomicIntegerFieldUpdaterImpl(java.util.concurrent.atomic.AtomicIntegerFieldUpdater<T> updater) {
            this.updater = updater;
        }

        @Override
        public boolean compareAndSet(T obj, int expect, int update) {
            return updater.compareAndSet(obj, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, int expect, int update) {
            return updater.weakCompareAndSet(obj, expect, update);
        }

        @Override
        public void set(T obj, int newValue) {
            updater.set(obj, newValue);
        }

        @Override
        public void lazySet(T obj, int newValue) {
            updater.lazySet(obj,newValue);
        }

        @Override
        public int get(T obj) {
            return updater.get(obj);
        }
    }
}