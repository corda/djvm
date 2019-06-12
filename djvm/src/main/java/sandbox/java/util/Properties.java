package sandbox.java.util;

import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.util.Properties}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Properties extends Hashtable<Object, Object> {

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
}
