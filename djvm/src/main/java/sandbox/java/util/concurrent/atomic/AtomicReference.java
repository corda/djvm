package sandbox.java.util.concurrent.atomic;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;
import sandbox.java.util.function.BinaryOperator;
import sandbox.java.util.function.UnaryOperator;

import java.io.Serializable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class AtomicReference<V> extends sandbox.java.lang.Object implements Serializable {

    private V value;

    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    public AtomicReference() {
    }

    public final V get() {
        return value;
    }

    public final void set(V newValue) {
        value = newValue;
    }

    public final void lazySet(V newValue) {
        set(newValue);
    }

    public final boolean compareAndSet(V expectedValue, V newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        } else {
            return false;
        }
    }

    public final boolean weakCompareAndSet(V expectedValue, V newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final V getAndSet(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }

    public final V getAndUpdate(@NotNull UnaryOperator<V> updater) {
        V oldValue = value;
        value = updater.apply(oldValue);
        return oldValue;
    }

    public final V updateAndGet(@NotNull UnaryOperator<V> updater) {
        return (value = updater.apply(value));
    }

    public final V getAndAccumulate(V x, @NotNull BinaryOperator<V> accumulator) {
        V oldValue = value;
        value = accumulator.apply(oldValue, x);
        return oldValue;
    }

    public final V accumulateAndGet(V x, @NotNull BinaryOperator<V> accumulator) {
        return (value = accumulator.apply(value, x));
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return String.valueOf(get());
    }
}
