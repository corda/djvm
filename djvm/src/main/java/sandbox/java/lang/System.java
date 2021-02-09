package sandbox.java.lang;

import net.corda.djvm.SandboxRuntimeContext;
import org.jetbrains.annotations.Nullable;
import sandbox.java.security.AccessControlException;

@SuppressWarnings("unused")
public final class System extends Object {
    private System() {}

    public static int identityHashCode(java.lang.Object obj) {
        int nativeHashCode = java.lang.System.identityHashCode(obj);
        return SandboxRuntimeContext.getInstance().getHashCodeFor(nativeHashCode);
    }

    public static final String lineSeparator = String.NEWLINE;

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static void arraycopy(java.lang.Object src, int srcPos, java.lang.Object dest, int destPos, int length) {
        java.lang.System.arraycopy(src, srcPos, dest, destPos, length);
    }

    public static long currentTimeMillis() {
        throw DJVM.failApi("java.lang.System.currentTimeMillis()");
    }

    public static long nanoTime() {
        throw DJVM.failApi("java.lang.System.nanoTime()");
    }

    public static String getProperty(String name, String defaultValue) {
        return defaultValue;
    }

    @Nullable
    public static String getProperty(String name) {
        return null;
    }

    @Nullable
    public static String setProperty(String name, String value) {
        return null;
    }

    @Nullable
    public static SecurityManager getSecurityManager() {
        return null;
    }

    public static void setSecurityManager(SecurityManager sm) {
        throw (RuntimeException) DJVM.fromDJVM(new AccessControlException(DJVM.intern("access denied")));
    }

    public static void runFinalization() {}
    public static void gc() {}
}
