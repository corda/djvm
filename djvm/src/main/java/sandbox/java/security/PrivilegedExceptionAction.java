package sandbox.java.security;

/**
 * This is a dummy class that implements just enough of {@link java.security.PrivilegedExceptionAction} to allow
 * us to compile {@link sandbox.java.security.AccessController}.
 */
public interface PrivilegedExceptionAction<T> {
    T run() throws Exception;
}
