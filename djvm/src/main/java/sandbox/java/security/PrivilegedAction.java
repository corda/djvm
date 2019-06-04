package sandbox.java.security;

/**
 * This is a dummy class that implements just enough of {@link java.security.PrivilegedAction} to allow
 * us to compile {@link sandbox.java.security.AccessController} and {@link sandbox.sun.security.action.GetPropertyAction}.
 */
public interface PrivilegedAction<T> {
    T run();
}
