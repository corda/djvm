package sandbox.java.security;

import sandbox.java.lang.DJVM;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unused", "RedundantThrows"})
public final class AccessController extends sandbox.java.lang.Object {
    private static final String FORBIDDEN_METHOD = "Zombie method invoked!";

    private AccessController() {}

    /**
     * This method should NEVER be invoked because we're supposed
     * to redirect all calls to the actual JVM method instead.
     * {@link java.security.AccessController#doPrivileged(java.security.PrivilegedAction)}
     * @param <T> Generic return type.
     * @param action The action to perform with elevated privileges.
     * @return T The result of the privileged action.
     */
    public static <T> T doPrivileged(@NotNull PrivilegedAction<T> action) {
        throw DJVM.fail(FORBIDDEN_METHOD);
    }

    /**
     * This method should NEVER be invoked because we're supposed
     * to redirect all calls to the actual JVM method instead.
     * {@link java.security.AccessController#doPrivileged(java.security.PrivilegedExceptionAction)}
     * @param <T> Generic return type.
     * @param action The action to perform with elevated privileges.
     * @return T The result of the privileged action.
     * @throws java.security.PrivilegedActionException Wraps any checked
     * exception that the privileged action may throw.
     */
    public static <T> T doPrivileged(@NotNull PrivilegedExceptionAction<T> action) throws java.security.PrivilegedActionException {
        throw DJVM.fail(FORBIDDEN_METHOD);
    }
}
