package sandbox.java.time.zone;

import sandbox.java.lang.String;
import sandbox.java.lang.Throwable;

/**
 * This is a dummy class that implements just enough of {@link java.time.zone.ZoneRulesException}
 * to allow us to compile {@link ZoneRulesProvider}.
 */
@SuppressWarnings("WeakerAccess")
public class ZoneRulesException extends Throwable {
    public ZoneRulesException(String message, Throwable cause) {
        super(message, cause);
    }
}
