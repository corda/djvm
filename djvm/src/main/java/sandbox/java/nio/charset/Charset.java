package sandbox.java.nio.charset;

import sandbox.java.nio.charset.spi.CharsetProvider;
import sandbox.java.util.Iterator;

/**
 * This is a dummy class that implements just enough of {@link java.nio.charset.Charset}
 * to allow us to compile {@link sandbox.java.lang.String}.
 */
@SuppressWarnings("unused")
public abstract class Charset extends sandbox.java.lang.Object {
    private final sandbox.java.lang.String canonicalName;

    protected Charset(sandbox.java.lang.String canonicalName, sandbox.java.lang.String[] aliases) {
        this.canonicalName = canonicalName;
    }

    public final sandbox.java.lang.String name() {
        return canonicalName;
    }

    /**
     * The DJVM contains no extended character sets, so prevent
     * it from searching for any. This nested class is copied
     * into the sandbox as a template.
     */
    private static class ExtendedProviderHolder {
        static final CharsetProvider extendedProvider = null;
    }

    /**
     * The DJVM cannot access any providers either, so copy this
     * nested Iterator into the sandbox as another template.
     */
    private static Iterator<CharsetProvider> providers() {
        return new Iterator<CharsetProvider>() {
            @Override
            public boolean hasNext() {
                return false;
            }
            @Override
            public CharsetProvider next() {
                throw new UnsupportedOperationException();
            }
        };
    }
}