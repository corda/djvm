package net.corda.djvm;

@FunctionalInterface
public interface ExceptionalObjectLongIntConsumer<T> {
    void accept(T item, long value1, int value2) throws Exception;
}
