package sandbox.sun.security.x509;

import org.jetbrains.annotations.NotNull;
import sandbox.java.security.PrivilegedExceptionAction;
import sandbox.javax.security.auth.x500.DJVM;
import sandbox.javax.security.auth.x500.X500Principal;

/**
 * This is a dummy class. What we're actually reimplementing here is
 * the anonymous inner class.
 */
@SuppressWarnings("unused")
public class X500Name extends sandbox.java.lang.Object {
    static {
        /*
         * This is what we're really replacing here.
         */
        new PrivilegedExceptionAction<Object[]>() {
            @Override
            public Object[] run() {
                return new Object[]{ null, null };
            }
        };
    }

    private X500Principal x500Principal;

    /**
     * This method will be stitched into the actual {@link X500Name}
     * class at run-time. It is implemented here only for reference.
     * @return A new {@link X500Principal} wrapping this {@link X500Name}.
     */
    public X500Principal asX500Principal() {
        return DJVM.create(this);
    }

    /**
     * This method will be stitched into the actual {@link X500Name}
     * class at run-time. It is implemented here only for reference.
     * @param principal Any {@link X500Principal} object.
     * @return The {@link X500Name} contained within this {@link X500Principal}.
     */
    @NotNull
    public static X500Name asX500Name(X500Principal principal) {
        X500Name name = DJVM.unwrap(principal);
        name.x500Principal = principal;
        return name;
    }
}
