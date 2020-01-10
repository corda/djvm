package sandbox.java.util.concurrent.atomic;

import org.jetbrains.annotations.NotNull;
import sandbox.java.util.concurrent.atomic.AtomicIntegerFieldUpdater.AtomicIntegerFieldUpdaterImpl;
import sandbox.java.util.concurrent.atomic.AtomicLongFieldUpdater.AtomicLongFieldUpdaterImpl;
import sandbox.java.util.concurrent.atomic.AtomicReferenceFieldUpdater.AtomicReferenceFieldUpdaterImpl;

@SuppressWarnings("unused")
public final class DJVM {
    private DJVM() {
    }

    @NotNull
    public static <T,V> AtomicReferenceFieldUpdater<T,V> toDJVM(java.util.concurrent.atomic.AtomicReferenceFieldUpdater<T,V> ref) {
        return new AtomicReferenceFieldUpdaterImpl<>(ref);
    }

    @NotNull
    public static <T> AtomicLongFieldUpdater<T> toDJVM(java.util.concurrent.atomic.AtomicLongFieldUpdater<T> ref) {
        return new AtomicLongFieldUpdaterImpl<>(ref);
    }

    @NotNull
    public static <T> AtomicIntegerFieldUpdater<T> toDJVM(java.util.concurrent.atomic.AtomicIntegerFieldUpdater<T> ref) {
        return new AtomicIntegerFieldUpdaterImpl<>(ref);
    }
}
