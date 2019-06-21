package sandbox.java.util.concurrent.atomic;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.Number;
import sandbox.java.util.function.LongBinaryOperator;
import sandbox.java.util.function.LongUnaryOperator;
import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AtomicLong extends Number implements Serializable {
    private long value;

    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    public AtomicLong() {
    }

    public final long get() {
        return value;
    }

    public final void set(long newValue) {
        value = newValue;
    }

    public final void lazySet(long newValue) {
        value = newValue;
    }

    public final long getAndSet(long newValue) {
        long oldValue = value;
        value = newValue;
        return oldValue;
    }

    public final boolean compareAndSet(long expectedValue, long newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        } else {
            return false;
        }
    }

    public final boolean weakCompareAndSet(long expectedValue, long newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final long getAndIncrement() {
        long oldValue = value;
        ++value;
        return oldValue;
    }

    public final long getAndDecrement() {
        long oldValue = value;
        --value;
        return oldValue;
    }

    public final long getAndAdd(long delta) {
        long oldValue = value;
        value += delta;
        return oldValue;
    }

    public final long incrementAndGet() {
        return ++value;
    }

    public final long decrementAndGet() {
        return --value;
    }

    public final long addAndGet(long delta) {
        return (value += delta);
    }

    public final long getAndUpdate(LongUnaryOperator updater) {
        long oldValue = value;
        value = updater.applyAsLong(oldValue);
        return oldValue;
    }

    public final long updateAndGet(LongUnaryOperator updater) {
        return (value = updater.applyAsLong(value));
    }

    public final long getAndAccumulate(long x, LongBinaryOperator accumulator) {
        long oldValue = value;
        value = accumulator.applyAsLong(oldValue, x);
        return oldValue;
    }

    public final long accumulateAndGet(long x, LongBinaryOperator accumulator) {
        return (value = accumulator.applyAsLong(value, x));
    }

    @Override
    public double doubleValue() {
        return (double) value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public short shortValue() {
        return (short) value;
    }

    @Override
    public byte byteValue() {
        return (byte) value;
    }

    @Override
    @NotNull
    protected final java.util.concurrent.atomic.AtomicLong fromDJVM() {
        return new java.util.concurrent.atomic.AtomicLong(value);
    }

    @Override
    @NotNull
    public java.lang.String toString() {
        return java.lang.Long.toString(value);
    }
}
