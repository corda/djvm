package sandbox.java.util;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.util.LinkedHashMap}
 * to allow us to compile {@link sandbox.java.lang.DJVM} and {@link sandbox.java.util.concurrent.ConcurrentHashMap}.
 */
public class LinkedHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    private final java.util.LinkedHashMap<K, V> map;

    public LinkedHashMap(int initialCapacity, float loadFactor) {
        map = new java.util.LinkedHashMap<>(initialCapacity, loadFactor);
    }

    public LinkedHashMap(int initialCapacity) {
        map = new java.util.LinkedHashMap<>(initialCapacity);
    }

    public LinkedHashMap() {
        map = new java.util.LinkedHashMap<>();
    }

    public LinkedHashMap(Map<? extends K, ? extends V> map) {
        this.map = new java.util.LinkedHashMap<>();
        putAll(map);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entries = new HashSet<>();
        for (java.util.Map.Entry<K, V> entry : map.entrySet()) {
            entries.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            this.map.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V remove(Object o) {
        return map.remove(o);
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
    public void clear() {
        map.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else {
            @SuppressWarnings("unchecked")
            LinkedHashMap<K, V> other = (LinkedHashMap<K, V>) obj;
            return map.equals(other.map);
        }
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
