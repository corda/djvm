package sandbox.java.lang.reflect;

/**
 * This is a dummy class that implements just enough of {@link java.lang.reflect.AnnotatedType}
 * to allow us to compile {@link sandbox.java.lang.reflect.Executable}, etc.
 */
public interface AnnotatedType extends AnnotatedElement {
    Type getType();
}
