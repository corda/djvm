package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.LinkedHashMap}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
public class LinkedHashMap<K, V> extends sandbox.java.lang.Object implements Map<K, V> {
    private final java.util.LinkedHashMap<K, V> map;

    public LinkedHashMap(int initialSize) {
        map = new java.util.LinkedHashMap<>(initialSize);
    }

    public LinkedHashMap() {
        map = new java.util.LinkedHashMap<>();
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }
}
