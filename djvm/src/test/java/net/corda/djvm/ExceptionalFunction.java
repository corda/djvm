package net.corda.djvm;

@FunctionalInterface
public interface ExceptionalFunction<T, R> {
    R apply(T input) throws Exception;
}
