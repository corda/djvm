package sandbox.java.util.concurrent;

import sandbox.java.util.Map;

/**
 * This is a dummy class that implements just enough of {@link java.util.concurrent.ConcurrentMap}
 * to allow us to compile {@link sandbox.java.util.concurrent.ConcurrentHashMap}.
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {
    @Override
    V putIfAbsent(K key, V value);

    @Override
    boolean remove(Object key, Object value);

    @Override
    V replace(K key, V value);

    @Override
    boolean replace(K key, V oldValue, V newValue);
}
