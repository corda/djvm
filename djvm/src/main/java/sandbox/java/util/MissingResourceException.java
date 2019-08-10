package sandbox.java.util;

import sandbox.java.lang.String;
import sandbox.java.lang.Throwable;

/**
 * This is a dummy class that implements just enough of {@link java.util.MissingResourceException}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings("unused")
public class MissingResourceException extends Throwable {
    public MissingResourceException(String baseName, String className, String key) {
    }
}
