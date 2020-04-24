package net.corda.djvm;

@FunctionalInterface
public interface ExceptionalObjectLongConsumer<T> {
    void accept(T item, long value) throws Exception;
}
