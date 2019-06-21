package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Map}
 * to allow us to compile both {@link sandbox.java.lang.DJVM} and
 * {@link sandbox.java.util.concurrent.ConcurrentHashMap}.
 */
public interface Map<K, V> {
    V put(K key, V value);

    void putAll(Map<? extends K, ? extends V> map);

    V get(K key);

    V remove(Object key);

    boolean equals(Object obj);

    int hashCode();

    int size();

    boolean isEmpty();

    void clear();

    Set<K> keySet();

    Collection<V> values();

    Set<Map.Entry<K, V>> entrySet();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    @SuppressWarnings("unused")
    interface Entry<K, V> {
        K getKey();

        V getValue();

        V setValue(V value);

        boolean equals(Object obj);

        int hashCode();
    }

    default V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException("Dummy class - not implemented here");
    }

    default boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("Dummy class - not implemented here");
    }

    default V replace(K key, V value) {
        throw new UnsupportedOperationException("Dummy class - not implemented here");
    }

    default boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException("Dummy class - not implemented here");
    }
}
