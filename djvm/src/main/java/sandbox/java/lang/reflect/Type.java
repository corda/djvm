package sandbox.java.lang.reflect;

import sandbox.java.lang.Object;
import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.lang.reflect.Type}
 * to allow us to compile {@link sandbox.java.lang.reflect.Executable}, etc.
 */
public interface Type {
    default String getTypeName() {
        return ((Object)this).toDJVMString();
    }
}
