package sandbox.javax.security.auth.x500;

import org.jetbrains.annotations.NotNull;
import sandbox.sun.security.x509.X500Name;

@SuppressWarnings("unused")
public final class DJVM {
    private DJVM() {}

    @NotNull
    public static X500Principal create(X500Name name) {
        return new X500Principal(name);
    }

    public static X500Name unwrap(@NotNull X500Principal principal) {
        return principal.unwrap();
    }
}
