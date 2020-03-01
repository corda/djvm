package sandbox.java.security;

import sandbox.java.lang.Exception;
import sandbox.java.lang.Throwable;

/**
 * This is a dummy class that implements just enough of {@link java.security.PrivilegedActionException}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
public class PrivilegedActionException extends Exception {
    private final Exception exception;

    public PrivilegedActionException(Exception exception) {
        super((Throwable) null);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public Throwable getCause() {
        return exception;
    }
}
