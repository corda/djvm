package net.corda.djvm;

@FunctionalInterface
public interface ExceptionalConsumer<T> {
    void accept(T obj) throws Exception;
}
