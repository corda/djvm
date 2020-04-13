package sandbox.java.lang.reflect;

/**
 * This is a dummy class that implements just enough of {@link java.lang.reflect.GenericDeclaration}
 * to allow us to compile {@link sandbox.java.lang.reflect.Executable}, etc.
 */
@SuppressWarnings("unused")
public interface GenericDeclaration extends AnnotatedElement {
    TypeVariable<?>[] getTypeParameters();
}
