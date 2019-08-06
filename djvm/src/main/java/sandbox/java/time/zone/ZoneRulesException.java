package sandbox.java.time.zone;

import sandbox.java.lang.String;
import sandbox.java.lang.Throwable;

public class ZoneRulesException extends Throwable {
    ZoneRulesException(String message, Throwable cause) {
        super(message, cause);
    }
}
