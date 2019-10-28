package sandbox.java.util;

import sandbox.java.io.InputStream;
import sandbox.java.lang.String;

import java.io.IOException;

/**
 * This is a dummy class that implements just enough of {@link java.util.Properties}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings({"RedundantThrows", "WeakerAccess", "unused"})
public final class Properties extends Hashtable<Object, Object> {
    private static final java.lang.String NOT_IMPLEMENTED = "Dummy class - not implemented";

    public Object setProperty(String key, String value) {
        return put(key, value);
    }

    public String getProperty(String key) {
        return (String) get(key);
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value == null) ? defaultValue : value;
    }

    public void load(InputStream inStream) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
