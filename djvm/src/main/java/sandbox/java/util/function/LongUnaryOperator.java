package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.LongUnaryOperator}
 * to allow us to compile {@link sandbox.java.util.concurrent.atomic.AtomicLong}.
 */
@FunctionalInterface
public interface LongUnaryOperator {
    long applyAsLong(long value);
}
