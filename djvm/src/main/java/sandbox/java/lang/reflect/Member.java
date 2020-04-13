package sandbox.java.lang.reflect;

import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.lang.reflect.Member}
 * to allow us to compile {@link sandbox.java.lang.reflect.Executable}, etc.
 */
@SuppressWarnings("unused")
public interface Member {
    Class<?> getDeclaringClass();
    String getName();
    int getModifiers();
    boolean isSynthetic();
}
