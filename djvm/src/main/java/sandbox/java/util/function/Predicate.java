package sandbox.java.util.function;

/**
 * This is a dummy class that implements just enough of {@link java.util.function.Predicate}
 * to allow us to compile {@link sandbox.PredicateTask}.
 */
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T obj);
}
