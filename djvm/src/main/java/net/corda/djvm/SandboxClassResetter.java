package net.corda.djvm;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import static java.security.AccessController.doPrivileged;
import static org.objectweb.asm.Opcodes.ACC_FINAL;

/**
 * Classes that contain static fields need to have these fields
 * re-initialised before a sandbox can be reused. The DJVM has
 * rewritten these classes to contain a special reset method that
 * will set all of their static fields to zero before repeating
 * the work of their {@literal <clinit>} functions.
 */
final class SandboxClassResetter {
    private static final Field MODIFIERS_FIELD;

    /*
     * This approach has been blocked in Java 12 by adding Field.modifiers
     * to a list of "banned" fields which cannot be reached via reflection.
     * The alternative is to create a MethodHandle for this field's setter
     * method via a private Lookup object. However, those APIs were only
     * introduced in Java 9, which forces us to use reflection on Java 8.
     */
    static {
        try {
            MODIFIERS_FIELD = doPrivileged((PrivilegedExceptionAction<Field>) () -> {
                Field field = Field.class.getDeclaredField("modifiers");
                field.setAccessible(true);
                return field;
            });
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            throw new InternalError(cause.getMessage(), cause);
        }
    }

    private final CallSite resetSite;
    private final MethodHandle resetHandle;

    SandboxClassResetter() {
        resetSite = new MutableCallSite(MethodType.methodType(void.class));
        resetHandle = resetSite.dynamicInvoker();
    }

    void reset(@NotNull Resettables resettables) throws Throwable {
        for (Resettable resettable : resettables.getResettables()) {
            unlock(resettable.getFinalFields());
            try {
                if (resettable.hasArgs()) {
                    resettable.invokeWithArgs();
                } else {
                    resetSite.setTarget(resettable.getResetMethod());
                    resetHandle.invokeExact();
                }
            } finally {
                lock(resettable.getFinalFields());
            }
        }
    }

    private void unlock(@NotNull List<Field> fields) throws IllegalAccessException {
        for (Field field : fields) {
            MODIFIERS_FIELD.setInt(field, field.getModifiers() & ~ACC_FINAL);
        }
    }

    private void lock(@NotNull List<Field> fields) throws IllegalAccessException {
        for (Field field : fields) {
            MODIFIERS_FIELD.setInt(field, field.getModifiers() | ACC_FINAL);
        }
    }
}
