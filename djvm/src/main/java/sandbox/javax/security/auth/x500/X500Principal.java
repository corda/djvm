package sandbox.javax.security.auth.x500;

import sandbox.sun.security.x509.X500Name;

/**
 * This is a dummy class that implements just enough of {@link javax.security.auth.x500.X500Principal} to allow
 * us to compile {@link sandbox.javax.security.auth.x500.DJVM}.
 */
@SuppressWarnings("unused")
public final class X500Principal extends sandbox.java.lang.Object {
    private transient X500Name thisX500Name;

    X500Principal(X500Name name) {
    }

    /**
     * This method will be stitched into the actual {@link X500Principal}
     * class at run-time. It is implemented here only for reference.
     * @return internal {@link X500Name} value.
     */
    final X500Name unwrap() {
        return thisX500Name;
    }
}
