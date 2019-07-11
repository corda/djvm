package sandbox.java.util;

import org.jetbrains.annotations.NotNull;

/**
 * This is a dummy class that implements just enough of {@link java.util.ArrayList}
 * to allow us to compile {@link sandbox.java.util.concurrent.locks.ReentrantLock}.
 */
public class ArrayList<T> extends sandbox.java.lang.Object implements List<T> {
    private static final String UNSUPPORTED = "Dummy class - not implemented";
    private final java.util.List<T> list;

    public ArrayList(int initialCapacity) {
        list = new java.util.ArrayList<>(initialCapacity);
    }

    public ArrayList() {
        list = new java.util.ArrayList<>();
    }

    @Override
    @NotNull
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public boolean add(T item) {
        return list.add(item);
    }

    @Override
    public boolean addAll(Collection<? extends T> collect) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return list.contains(obj);
    }

    @Override
    public boolean containsAll(Collection<?> collect) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public boolean remove(Object obj) {
        return list.remove(obj);
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
        return list.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return list.toArray(a);
    }
}
