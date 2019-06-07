package sandbox.java.security;

import org.jetbrains.annotations.NotNull;

public final class AccessController extends sandbox.java.lang.Object {

    private AccessController() {
    }

    @SuppressWarnings("unused")
    public static <T> T doPrivileged(@NotNull PrivilegedAction<T> action) {
        return action.run();
    }
}
