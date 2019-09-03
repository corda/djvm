package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.BinaryOperator}
 * to allow us to compile {@link sandbox.java.util.concurrent.atomic.AtomicReference}.
 */
@FunctionalInterface
public interface BinaryOperator<T> {
    T apply(T left, T right);
}
