package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Collection}
 * to allow us to compile {@link sandbox.java.util.AbstractMap}.
 */
public interface Collection<T> extends sandbox.java.lang.Iterable<T> {

    boolean add(T item);

    boolean addAll(Collection<? extends T> c);

    void clear();

    boolean contains(Object obj);

    boolean containsAll(Collection<?> c);

    boolean equals(Object obj);

    int hashCode();

    boolean isEmpty();

    boolean remove(Object o);

    boolean removeAll(Collection<?> c);

    boolean retainAll(Collection<?> c);

    int size();

    Object[] toArray();

    <E> E[] toArray(E[] a);
}
