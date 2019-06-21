package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.LongBinaryOperator}
 * to allow us to compile {@link sandbox.java.util.concurrent.atomic.AtomicLong}.
 */
@FunctionalInterface
public interface LongBinaryOperator {
    long applyAsLong(long left, long right);
}
