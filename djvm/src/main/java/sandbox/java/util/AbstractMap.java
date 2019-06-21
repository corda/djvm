package sandbox.java.util;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.util.AbstractMap}
 * to allow us to compile {@link sandbox.java.util.LinkedHashMap} and
 * {@link sandbox.java.util.concurrent.ConcurrentHashMap}.
 */
public abstract class AbstractMap<K, V> extends sandbox.java.lang.Object implements Map<K, V> {

    protected AbstractMap() {
    }

    @Override
    public abstract Set<Map.Entry<K,V>> entrySet();

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }

    @Override
    public Set<K> keySet() {
        Set<Entry<K, V>> entries = entrySet();
        HashSet<K> keys = new HashSet<>(entries.size());
        for (Entry<K, V> entry : entries) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    public static class SimpleEntry<K,V> extends sandbox.java.lang.Object implements Entry<K, V>, Serializable {
        private final K key;
        private V value;

        SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof Entry)) {
                return false;
            } else {
                @SuppressWarnings("unchecked")
                Entry<K, V> other = (Entry<K, V>) obj;
                return java.util.Objects.equals(key, other.getKey())
                        && java.util.Objects.equals(value, other.getValue());
            }
        }
    }
}
