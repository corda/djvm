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
import java.util.LinkedList;
import java.util.List;

import static java.security.AccessController.doPrivileged;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
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

    private final LinkedList<Resettable> resettables;
    private final CallSite resetSite;
    private final MethodHandle resetHandle;

    SandboxClassResetter() {
        resettables = new LinkedList<>();
        resetSite = new MutableCallSite(MethodType.methodType(void.class));
        resetHandle = resetSite.dynamicInvoker();
    }

    void add(MethodHandle resetMethod, @NotNull List<Field> finalFields) {
        synchronized(resettables) {
            resettables.add(new Resettable(resetMethod, finalFields.isEmpty() ? emptyList() : unmodifiableList(finalFields)));
        }
    }

    void add(MethodHandle resetMethod) {
        synchronized(resettables) {
            resettables.add(new Resettable(resetMethod, emptyList()));
        }
    }

    synchronized void reset() throws Throwable {
        for (Resettable resettable : getSnapshotOfResettables()) {
            unlock(resettable.getFinalFields());
            try {
                resetSite.setTarget(resettable.getResetMethod());
                resetHandle.invokeExact();
            } finally {
                lock(resettable.getFinalFields());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Resettable> getSnapshotOfResettables() {
        // Create a snapshot of the resettables that we can iterate
        // over without risking a concurrent modification exception.
        // Cloning a LinkedList is a trivial O(1) operation, whereas
        // copy-constructing it would be O(N).
        //
        // We are assuming that the only possible modification would
        // be some new items being added to the end. But this would
        // only be likely during interactive debugging anyway.
        synchronized(resettables) {
            return (List<Resettable>) resettables.clone();
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
