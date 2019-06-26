package sandbox.java.security;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class AccessController extends sandbox.java.lang.Object {

    private AccessController() {
    }

    public static <T> T doPrivileged(@NotNull PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(@NotNull PrivilegedExceptionAction<T> action) throws Exception {
        return action.run();
    }
}
