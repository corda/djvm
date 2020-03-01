package sandbox.java.security;

import net.corda.djvm.rules.RuleViolationError;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unused", "RedundantThrows"})
public final class AccessController extends sandbox.java.lang.Object {
    private static final String FORBIDDEN_METHOD = "Zombie method invoked!";

    private AccessController() {}

    /**
     * This method should NEVER be invoked because we're supposed
     * to redirect all calls to the actual JVM method instead.
     * {@link java.security.AccessController#doPrivileged(java.security.PrivilegedAction)}
     */
    public static <T> T doPrivileged(@NotNull PrivilegedAction<T> action) {
        throw new RuleViolationError(FORBIDDEN_METHOD);
    }

    /**
     * This method should NEVER be invoked because we're supposed
     * to redirect all calls to the actual JVM method instead.
     * {@link java.security.AccessController#doPrivileged(java.security.PrivilegedExceptionAction)}
     */
    public static <T> T doPrivileged(@NotNull PrivilegedExceptionAction<T> action) throws java.security.PrivilegedActionException {
        throw new RuleViolationError(FORBIDDEN_METHOD);
    }
}
