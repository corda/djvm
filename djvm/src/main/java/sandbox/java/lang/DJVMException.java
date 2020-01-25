package sandbox.java.lang;

/**
 * All synthetic {@link java.lang.Throwable} classes wrapping non-JVM exceptions
 * will implement this interface.
 */
@FunctionalInterface
public interface DJVMException {
    /**
     * Returns the {@link sandbox.java.lang.Throwable} instance inside the wrapper.
     * @return The non-throwable {@link Throwable}.
     */
    Throwable getThrowable();
}
