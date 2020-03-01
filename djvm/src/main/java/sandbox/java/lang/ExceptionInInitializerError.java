package sandbox.java.lang;

/**
 * This is a dummy class that implements just enough of {@link java.lang.ExceptionInInitializerError}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public class ExceptionInInitializerError extends Throwable {
    private final Throwable exception;

    public ExceptionInInitializerError(Throwable t) {
        super((Throwable) null);
        exception = t;
    }

    public ExceptionInInitializerError(String message) {
        super(message);
        exception = null;
    }

    public Throwable getException() {
        return exception;
    }

    public Throwable getCause() {
        return exception;
    }
}
