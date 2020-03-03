package sandbox.javax.security.auth.x500;

import sandbox.sun.security.x509.X500Name;

@SuppressWarnings("unused")
public final class DJVM {
    private DJVM() {}

    public static X500Principal create(X500Name name) {
        return new X500Principal(name);
    }

    public static X500Name unwrap(X500Principal principal) {
        return principal.unwrap();
    }
}
