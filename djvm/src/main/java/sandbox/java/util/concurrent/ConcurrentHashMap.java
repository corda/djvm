package sandbox.java.util.concurrent;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.Boolean;
import sandbox.java.util.*;

import java.io.Serializable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
    private final LinkedHashMap<K, V> map;
    private transient KeySetView<K,V> keySet;

    public ConcurrentHashMap() {
        map = new LinkedHashMap<>();
    }

    public ConcurrentHashMap(int initialSize) {
        map = new LinkedHashMap<>(initialSize);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor);
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> map) {
        this.map = new LinkedHashMap<>(map);
    }

    LinkedHashMap<K, V> unwrap() {
        return map;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public KeySetView<K, V> keySet() {
        KeySetView<K, V> keys = keySet;
        return (keys != null) ? keys : (keySet = new KeySetView<>(this, null));
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        this.map.putAll(map);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V remove(Object obj) {
        return map.remove(obj);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object obj) {
        return map.containsKey(obj);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(key, value);
    }

    @Override
    public V replace(K key, V value) {
        return map.replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return map.replace(key, oldValue, newValue);
    }

    public Enumeration<K> keys() {
        return new BaseEnumerator<>(keySet().iterator());
    }

    public Enumeration<V> elements() {
        return new BaseEnumerator<>(values().iterator());
    }

    public long mappingCount() {
        return map.size();
    }

    @NotNull
    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<>(new ConcurrentHashMap<>(), Boolean.TRUE);
    }

    @NotNull
    public static <K> KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<>(new ConcurrentHashMap<>(initialCapacity), Boolean.TRUE);
    }

    public KeySetView<K, V> keySet(V mappedValue) {
        if (mappedValue == null) {
            throw new NullPointerException();
        }
        return new KeySetView<>(this, mappedValue);
    }

    /**
     * This is a dummy class that implements just enough of {@link CollectionView}
     * to allow us to compile {@link sandbox.java.util.concurrent.ConcurrentHashMap.KeySetView}.
     */
    abstract static class CollectionView<K,V,E> extends sandbox.java.lang.Object implements Collection<E>, Serializable {
        private static final String UNSUPPORTED = "Dummy class - not implemented";
        final ConcurrentHashMap<K,V> map;

        CollectionView(ConcurrentHashMap<K,V> map) {
            this.map = map;
        }

        public ConcurrentHashMap<K,V> getMap() {
            return map;
        }

        @Override
        public final void clear() {
            map.clear();
        }

        @Override
        public final int size() {
            return map.size();
        }

        @Override
        public final boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        @NotNull
        public abstract Iterator<E> iterator();

        @Override
        public abstract boolean contains(Object o);

        @Override
        public abstract boolean remove(Object o);

        @Override
        public final Object[] toArray() {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        @Override
        public final <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        @Override
        @NotNull
        public final sandbox.java.lang.String toDJVMString() {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        @Override
        public final boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        @Override
        public final boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        @Override
        public final boolean retainAll(Collection<?> collect) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
    }

    static final class BaseEnumerator<K, V> extends sandbox.java.lang.Object implements Enumeration<K> {
        private final Iterator<K> iterator;

        BaseEnumerator(Iterator<K> iterator) {
            this.iterator = iterator;
        }

        @Override
        public final boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public final K nextElement() {
            return iterator.next();
        }
    }

    public static class KeySetView<K,V> extends CollectionView<K,V,K> implements Set<K>, Serializable {
        private final V value;

        KeySetView(ConcurrentHashMap<K,V> map, V value) {
            super(map);
            this.value = value;
        }

        public V getMappedValue() {
            return value;
        }

        @Override
        public boolean contains(Object obj) {
            return map.containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return map.remove(obj) != null;
        }

        @Override
        @NotNull
        public Iterator<K> iterator() {
            return map.unwrap().keySet().iterator();
        }

        @Override
        public boolean add(K key) {
            V v = value;
            if (v == null) {
                throw new UnsupportedOperationException();
            }
            return map.putIfAbsent(key, v) == null;
        }

        @Override
        public boolean addAll(Collection<? extends K> collect) {
            boolean added = false;
            V v = value;
            if (v == null) {
                throw new UnsupportedOperationException();
            }
            for (K key: collect) {
                if (map.putIfAbsent(key, v) == null) {
                    added = true;
                }
            }
            return added;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (K key: map.unwrap().keySet()) {
                hash += key.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof Set<?>)) {
                return false;
            } else {
                Set<?> other = (Set<?>) obj;
                return containsAll(other) && other.containsAll(this);
            }
        }
    }
}