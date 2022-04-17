package net.corda.djvm;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/**
 * Classes that contain static fields need to have these fields
 * re-initialised before a sandbox can be reused. The DJVM has
 * rewritten these classes to contain a special reset method that
 * will set all of their static fields to zero before repeating
 * the work of their {@literal <clinit>} functions.
 */
final class SandboxClassResetter {
    private final CallSite resetSite;
    private final MethodHandle resetHandle;

    SandboxClassResetter() {
        resetSite = new MutableCallSite(MethodType.methodType(void.class));
        resetHandle = resetSite.dynamicInvoker();
    }

    void reset(@NotNull Resettables resettables) throws Throwable {
        for (Resettable resettable : resettables.getResettables()) {
            resetSite.setTarget(resettable.getResetMethod());
            resetHandle.invokeExact();
        }
    }
}
