package sandbox.java.lang.reflect;

import sandbox.java.lang.annotation.Annotation;

/**
 * This is a dummy class that implements just enough of {@link java.lang.reflect.AnnotatedElement}
 * to allow us to compile {@link sandbox.java.lang.reflect.AccessibleObject}, etc.
 */
@SuppressWarnings("unused")
public interface AnnotatedElement {
    java.lang.String UNSUPPORTED = "Dummy class - not implemented";

    <T extends Annotation> T getAnnotation(Class<T> annotationType);
    Annotation[] getAnnotations();
    Annotation[] getDeclaredAnnotations();

    default boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationType) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationType) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationType) {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }
}
