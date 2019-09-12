package net.corda.djvm;

public interface Action<T, R> {
    T action(R input);
}
