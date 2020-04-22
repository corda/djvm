package sandbox.java.lang.annotation;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.lang.annotation.Annotation}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public interface Annotation {
    @Override
    @NotNull
    java.lang.String toString();

    @NotNull
    String toDJVMString();

    Class<? extends Annotation> annotationType();

    /**
     * This function will be stitched here at runtime.
     * @return Either the underlying annotation proxy created
     * by the JVM, or null if implemented from user code.
     */
    default java.lang.annotation.Annotation jvmAnnotation() {
        return null;
    }
}
