package sandbox.java.util.concurrent.atomic;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater}
 * to allow us to compile {@link AtomicReferenceFieldUpdaterImpl}.
 */
@SuppressWarnings("unused")
public abstract class AtomicReferenceFieldUpdater<T,V> extends sandbox.java.lang.Object {
    public abstract boolean compareAndSet(T obj, V expect, V update);

    public abstract boolean weakCompareAndSet(T obj, V expect, V update);

    public abstract void set(T obj, V newValue);

    public abstract void lazySet(T obj, V newValue);

    public abstract V get(T obj);

    /**
     * Wrap the native {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater} field inside
     * this because we cannot replicate {@link sun.reflect.Reflection#getCallerClass()} inside the
     * sandbox.
     */
    static final class AtomicReferenceFieldUpdaterImpl<T,V> extends AtomicReferenceFieldUpdater<T,V> {
        @SuppressWarnings("AtomicFieldUpdaterNotStaticFinal")
        private final java.util.concurrent.atomic.AtomicReferenceFieldUpdater<T, V> updater;

        AtomicReferenceFieldUpdaterImpl(java.util.concurrent.atomic.AtomicReferenceFieldUpdater<T, V> updater) {
            this.updater = updater;
        }

        @Override
        public boolean compareAndSet(T obj, V expect, V update) {
            return updater.compareAndSet(obj, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, V expect, V update) {
            return updater.weakCompareAndSet(obj, expect, update);
        }

        @Override
        public void set(T obj, V newValue) {
            updater.set(obj, newValue);
        }

        @Override
        public void lazySet(T obj, V newValue) {
            updater.lazySet(obj, newValue);
        }

        @Override
        public V get(T obj) {
            return updater.get(obj);
        }
    }
}
