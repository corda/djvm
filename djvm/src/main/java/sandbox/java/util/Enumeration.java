package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Enumeration}
 * to allow us to compile both {@link sandbox.java.util.concurrent.ConcurrentHashMap}
 * and {@link sandbox.java.lang.DJVMClassLoader}.
 */
@SuppressWarnings("unused")
public interface Enumeration<T> {
    boolean hasMoreElements();
    T nextElement();
}
