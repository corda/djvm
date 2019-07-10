package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.IntBinaryOperator}
 * to allow us to compile {@link sandbox.java.util.concurrent.atomic.AtomicInteger}.
 */
@FunctionalInterface
public interface IntBinaryOperator {
    int applyAsInt(int left, int right);
}
