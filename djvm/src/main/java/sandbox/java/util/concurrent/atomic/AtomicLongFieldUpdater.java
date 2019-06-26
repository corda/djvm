package sandbox.java.util.concurrent.atomic;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.atomic.AtomicLongFieldUpdater}
 * to allow us to compile {@link AtomicLongFieldUpdaterImpl}.
 */
@SuppressWarnings("unused")
public abstract class AtomicLongFieldUpdater<T> extends sandbox.java.lang.Object {
    public abstract boolean compareAndSet(T obj, long expect, long update);

    public abstract boolean weakCompareAndSet(T obj, long expect, long update);

    public abstract void set(T obj, long newValue);

    public abstract void lazySet(T obj, long newValue);

    public abstract long get(T obj);

    /**
     * Wrap the native {@link java.util.concurrent.atomic.AtomicLongFieldUpdater} field inside
     * this because we cannot replicate {@link sun.reflect.Reflection#getCallerClass()} inside the
     * sandbox.
     */
    static final class AtomicLongFieldUpdaterImpl<T> extends AtomicLongFieldUpdater<T> {
        @SuppressWarnings("AtomicFieldUpdaterNotStaticFinal")
        private final java.util.concurrent.atomic.AtomicLongFieldUpdater<T> updater;

        AtomicLongFieldUpdaterImpl(java.util.concurrent.atomic.AtomicLongFieldUpdater<T> updater) {
            this.updater = updater;
        }

        @Override
        public boolean compareAndSet(T obj, long expect, long update) {
            return updater.compareAndSet(obj, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, long expect, long update) {
            return updater.weakCompareAndSet(obj, expect, update);
        }

        @Override
        public void set(T obj, long newValue) {
            updater.set(obj, newValue);
        }

        @Override
        public void lazySet(T obj, long newValue) {
            updater.lazySet(obj, newValue);
        }

        @Override
        public long get(T obj) {
            return updater.get(obj);
        }
    }
}
