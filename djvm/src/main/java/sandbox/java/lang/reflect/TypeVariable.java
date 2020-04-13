package sandbox.java.lang.reflect;

import sandbox.java.lang.String;

import java.lang.reflect.AnnotatedType;

/**
 * This is a dummy class that implements just enough of {@link java.lang.reflect.TypeVariable}
 * to allow us to compile {@link sandbox.java.lang.reflect.Executable}, etc.
 */
@SuppressWarnings("unused")
public interface TypeVariable<T extends GenericDeclaration> extends Type, AnnotatedElement {
    Type[] getBounds();
    T getGenericDeclaration();
    String getName();
    AnnotatedType[] getAnnotatedBounds();
}
