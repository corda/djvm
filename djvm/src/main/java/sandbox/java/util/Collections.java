package sandbox.java.util;

/**
 * This is a dummy class that implements just enough of {@link java.util.Collections}
 * to allow us to compile {@link sandbox.java.util.ServiceLoader}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Collections extends sandbox.java.lang.Object {
    private static final String NOT_IMPLEMENTED = "Dummy class - not implemented";

    public static <T> Iterator<T> emptyIterator() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
