package sandbox.java.lang.annotation;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.lang.annotation.Annotation}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public interface Annotation {
    @NotNull
    String toDJVMString();

    @Override
    @NotNull
    java.lang.String toString();

    Class<? extends Annotation> annotationType();
}
