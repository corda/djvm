package sandbox.java.util.concurrent.atomic;

import sandbox.java.util.concurrent.atomic.AtomicIntegerFieldUpdater.AtomicIntegerFieldUpdaterImpl;
import sandbox.java.util.concurrent.atomic.AtomicLongFieldUpdater.AtomicLongFieldUpdaterImpl;
import sandbox.java.util.concurrent.atomic.AtomicReferenceFieldUpdater.AtomicReferenceFieldUpdaterImpl;

@SuppressWarnings("unused")
public final class DJVM {
    private DJVM() {
    }

    public static <T,V> AtomicReferenceFieldUpdater<T,V> toDJVM(java.util.concurrent.atomic.AtomicReferenceFieldUpdater<T,V> ref) {
        return new AtomicReferenceFieldUpdaterImpl<>(ref);
    }

    public static <T> AtomicLongFieldUpdater<T> toDJVM(java.util.concurrent.atomic.AtomicLongFieldUpdater<T> ref) {
        return new AtomicLongFieldUpdaterImpl<>(ref);
    }

    public static <T> AtomicIntegerFieldUpdater<T> toDJVM(java.util.concurrent.atomic.AtomicIntegerFieldUpdater<T> ref) {
        return new AtomicIntegerFieldUpdaterImpl<>(ref);
    }
}
