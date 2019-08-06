package sandbox.java.time;

import org.jetbrains.annotations.NotNull;
import sandbox.java.lang.String;

import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.time.ZoneId}
 * to allow us to compile {@link sandbox.java.time.ZoneOffset}.
 */
@SuppressWarnings("unused")
public abstract class ZoneId extends sandbox.java.lang.Object implements Serializable {
    public abstract String getId();

    @Override
    @NotNull
    protected final java.time.ZoneId fromDJVM() {
        return java.time.ZoneId.of(String.fromDJVM(getId()));
    }

    public static ZoneId of(String zoneId) {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }
}
