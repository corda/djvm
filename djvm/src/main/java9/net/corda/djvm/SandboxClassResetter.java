package net.corda.djvm;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
    private static final MethodHandle SET_MODIFIERS;

    /*
     * Create a MethodHandle for Field.modifiers' setter method.
     * We cannot rely on reflection to access this field. This
     * approach uses APIs introduced in Java 9.
     */
    static {
        try {
            SET_MODIFIERS = doPrivileged((PrivilegedExceptionAction<MethodHandle>) () -> {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
                return lookup.findSetter(Field.class, "modifiers", Integer.TYPE);
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

    private void unlock(@NotNull List<Field> fields) throws Throwable {
        for (Field field : fields) {
            SET_MODIFIERS.invokeExact(field, field.getModifiers() & ~ACC_FINAL);
        }
    }

    private void lock(@NotNull List<Field> fields) throws Throwable {
        for (Field field : fields) {
            SET_MODIFIERS.invokeExact(field, field.getModifiers() | ACC_FINAL);
        }
    }
}
