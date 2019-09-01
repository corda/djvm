package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Collections}
 * to allow us to compile {@link sandbox.java.net.URLConnection}.
 */
@SuppressWarnings("unused")
public class Collections {
    private static final String NOT_IMPLEMENTED = "Dummy class - not implemented.";

    private Collections() {}

    public static <K,V> Map<K,V> emptyMap() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public static <K,V> Map<K,V> unmodifiableMap(Map<K,V> map) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public static <V> List<V> emptyList() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public static <V> List<V> unmodifiableList(List<V> list) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public static <E> Enumeration<E> emptyEnumeration() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
