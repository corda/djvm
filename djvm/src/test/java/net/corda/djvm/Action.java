package net.corda.djvm;

@FunctionalInterface
public interface Action<T, R> {
    T action(R input);
}
