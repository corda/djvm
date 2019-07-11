package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.IntUnaryOperator}
 * to allow us to compile {@link sandbox.java.util.concurrent.atomic.AtomicInteger}.
 */
@FunctionalInterface
public interface IntUnaryOperator {
    int applyAsInt(int value);
}
