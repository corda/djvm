package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Map}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
public interface Map<K, V> {
    V put(K key, V value);

    V get(K key);
}
