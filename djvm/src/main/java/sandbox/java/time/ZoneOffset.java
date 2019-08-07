package sandbox.java.time;

import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.time.ZoneOffset}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public final class ZoneOffset extends ZoneId {
    @Override
    public String getId() {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }
}
