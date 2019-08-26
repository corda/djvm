package sandbox.java.util.concurrent.atomic;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.Integer;
import sandbox.java.lang.Number;
import sandbox.java.lang.String;
import sandbox.java.util.function.IntBinaryOperator;
import sandbox.java.util.function.IntUnaryOperator;

import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AtomicInteger extends Number implements Serializable {
    private int value;

    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
    }

    public final void lazySet(int newValue) {
        value = newValue;
    }

    public final int getAndSet(int newValue) {
        int oldValue = value;
        value = newValue;
        return oldValue;
    }

    public final boolean compareAndSet(int expectedValue, int newValue) {
        if (value == expectedValue) {
            value = newValue;
            return true;
        } else {
            return false;
        }
    }

    public final boolean weakCompareAndSet(int expectedValue, int newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final int getAndIncrement() {
        int oldValue = value;
        ++value;
        return oldValue;
    }

    public final int getAndDecrement() {
        int oldValue = value;
        --value;
        return oldValue;
    }

    public final int getAndAdd(int delta) {
        int oldValue = value;
        value += delta;
        return oldValue;
    }

    public final int incrementAndGet() {
        return ++value;
    }

    public final int decrementAndGet() {
        return --value;
    }

    public final int addAndGet(int delta) {
        return (value += delta);
    }

    public final int getAndUpdate(IntUnaryOperator updater) {
        int oldValue = value;
        value = updater.applyAsInt(oldValue);
        return oldValue;
    }

    public final int updateAndGet(IntUnaryOperator updater) {
        return (value = updater.applyAsInt(value));
    }

    public final int getAndAccumulate(int x, IntBinaryOperator accumulator) {
        int oldValue = value;
        value = accumulator.applyAsInt(oldValue, x);
        return oldValue;
    }

    public final int accumulateAndGet(int x, IntBinaryOperator accumulator) {
        return (value = accumulator.applyAsInt(value, x));
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
        return (long) value;
    }

    @Override
    public int intValue() {
        return value;
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
    protected final java.util.concurrent.atomic.AtomicInteger fromDJVM() {
        return new java.util.concurrent.atomic.AtomicInteger(value);
    }

    @Override
    @NotNull
    public String toDJVMString() {
        return Integer.toString(value);
    }
}
