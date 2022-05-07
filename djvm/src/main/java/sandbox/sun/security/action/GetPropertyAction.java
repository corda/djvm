package sandbox.sun.security.action;

import sandbox.java.lang.DJVM;
import sandbox.java.lang.String;
import sandbox.java.lang.System;
import sandbox.java.security.PrivilegedAction;
import sandbox.java.util.LinkedHashMap;
import sandbox.java.util.Map;

import java.lang.invoke.MethodHandles;
import java.util.function.BiConsumer;

public class GetPropertyAction extends sandbox.java.lang.Object implements PrivilegedAction<String> {

    private static final Map<String, String> systemValues = createSystemProperties();

    static {
        DJVM.forReset(MethodHandles.lookup(), "reset");
    }

    private static Map<String, String> createSystemProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(DJVM.intern("file.encoding"), DJVM.intern("UTF-8"));
        properties.put(DJVM.intern("user.language"), DJVM.intern("en"));
        properties.put(DJVM.intern("user.timezone"), DJVM.intern("UTC"));
        properties.put(DJVM.intern("line.separator"), System.lineSeparator);
        properties.put(DJVM.intern("path.separator"), DJVM.intern(":"));
        return properties;
    }

    @SuppressWarnings("unused")
    private static void reset(BiConsumer<Object, java.lang.String> resetter) {
        resetter.accept(createSystemProperties(), "systemValues");
    }

    private final String name;
    private final String defaultValue;

    @SuppressWarnings("WeakerAccess")
    public GetPropertyAction(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    @SuppressWarnings("unused")
    public GetPropertyAction(String name) {
        this(name, null);
    }

    @Override
    public String run() {
        String value = systemValues.get(name);
        return (value != null) ? value : defaultValue;
    }
}
