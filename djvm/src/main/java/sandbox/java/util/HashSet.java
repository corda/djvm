package sandbox.java.util;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("WeakerAccess")
public class HashSet<T> extends sandbox.java.lang.Object implements Set<T> {
    private static final String UNSUPPORTED = "Dummy class - not implemented";
    private final java.util.Set<T> set;

    public HashSet(int initialCapacity) {
        set = new java.util.HashSet<>(initialCapacity);
    }

    public HashSet() {
        set = new java.util.HashSet<>();
    }

    @Override
    @NotNull
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public boolean add(T item) {
        return set.add(item);
    }

    @Override
    public boolean addAll(Collection<? extends T> collect) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public void clear() {
        set.clear();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return set.contains(obj);
    }

    @Override
    public boolean containsAll(Collection<?> collect) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public boolean remove(Object obj) {
        return set.remove(obj);
    }

    @Override
    public boolean removeAll(Collection<?> collect) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public boolean retainAll(Collection<?> collect) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return set.toArray(a);
    }
}
