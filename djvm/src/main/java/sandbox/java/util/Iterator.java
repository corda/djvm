package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Iterator}
 * to allow us to compile {@link sandbox.java.nio.charset.Charset}.
 */
public interface Iterator<T> extends java.util.Iterator<T> {
    @Override
    boolean hasNext();

    @Override
    T next();
}
