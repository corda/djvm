package sandbox.java.security;

import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.security.AccessControlException}
 * to allow us to compile {@link sandbox.java.lang.System}.
 */
public class AccessControlException extends sandbox.java.lang.Throwable {
    public AccessControlException(String message) {
        super(message);
    }
}
