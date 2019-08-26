package sandbox.java.util;

import sandbox.java.lang.String;

/**
 * This is a dummy class that implements just enough of {@link java.util.ResourceBundle}
 * to allow us to compile {@link sandbox.java.lang.DJVM}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class ResourceBundle extends sandbox.java.lang.Object {
    private static final java.lang.String NOT_IMPLEMENTED = "Dummy class - not implemented";

    protected ResourceBundle parent;
    private String name;
    private Locale locale;

    protected abstract Object handleGetObject(String key);
    public abstract Enumeration<String> getKeys();

    public String getBaseBundleName() {
        return name;
    }

    public Locale getLocale() {
        return locale;
    }

    public final String getString(String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }

    public final Object getObject(String key) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public boolean containsKey(String key) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    public Set<String> keySet() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * Example implementation. The real one will be stitched here at runtime.
     * @param parent The {@link ResourceBundle} that will become this bundle's
     *               parent, assuming it doesn't already have a parent.
     */
    public void childOf(ResourceBundle parent) {
        if (this.parent == null) {
            this.parent = parent;
        }
    }

    /**
     * Example implementation. The real one will be stitched here at runtime.
     */
    public void init(String name, Locale locale) {
        this.name = name;
        this.locale = locale;
    }

    /**
     * This is a dummy class. We will load the actual one at runtime.
     */
    public static class Control {
        public List<String> getFormats(String baseName) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED);
        }

        public String toBundleName(String baseName, Locale locale) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED);
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED);
        }

        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED);
        }

        public static Control getControl(List<String> formats) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED);
        }

        public static Control getNoFallbackControl(List<String> formats) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED);
        }
    }
}
