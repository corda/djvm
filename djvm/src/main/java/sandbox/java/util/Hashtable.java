package sandbox.java.util;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.util.Hashtable}
 * to allow us to compile {@link sandbox.java.util.Properties}.
 */
public class Hashtable<K, V> extends sandbox.java.lang.Object implements Map<K, V>, Cloneable, Serializable {
    private final Map<K, V> table = new LinkedHashMap<>();

    @Override
    public V put(K key, V value) {
        return table.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        table.putAll(map);
    }

    @Override
    public V get(K key) {
        return table.get(key);
    }

    @Override
    public V remove(Object key) {
        return table.remove(key);
    }

    @Override
    public int size() {
        return table.size();
    }

    @Override
    public boolean isEmpty() {
        return table.isEmpty();
    }

    @Override
    public void clear() {
        table.clear();
    }

    @Override
    public Set<K> keySet() {
        return table.keySet();
    }

    @Override
    public Collection<V> values() {
        return table.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return table.entrySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return table.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return table.containsValue(value);
    }
}
