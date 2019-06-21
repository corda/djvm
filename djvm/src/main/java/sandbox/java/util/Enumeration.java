package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Enumeration}
 * to allow us to compile {@link sandbox.java.util.concurrent.ConcurrentHashMap}.
 */
public interface Enumeration<T> {
    boolean hasMoreElements();
    T nextElement();
}
