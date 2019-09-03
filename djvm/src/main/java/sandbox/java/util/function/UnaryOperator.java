package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.UnaryOperator}
 * to allow us to compile {@link sandbox.java.util.concurrent.atomic.AtomicReference}.
 */
@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {
    @Override
    T apply(T value);
}
