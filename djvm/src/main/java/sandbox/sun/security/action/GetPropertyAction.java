package sandbox.sun.security.action;

import sandbox.java.lang.DJVM;
import sandbox.java.lang.String;
import sandbox.java.lang.System;
import sandbox.java.security.PrivilegedAction;
import sandbox.java.util.LinkedHashMap;
import sandbox.java.util.Map;

@SuppressWarnings("unused")
public class GetPropertyAction extends sandbox.java.lang.Object implements PrivilegedAction<String> {

    private static Map<String, String> systemValues;
    static {
        systemValues = new LinkedHashMap<>();
        systemValues.put(DJVM.intern("file.encoding"), DJVM.intern("UTF-8"));
        systemValues.put(DJVM.intern("user.language"), DJVM.intern("en"));
        systemValues.put(DJVM.intern("user.timezone"), DJVM.intern("UTC"));
        systemValues.put(DJVM.intern("line.separator"), System.lineSeparator);
        systemValues.put(DJVM.intern("path.separator"), DJVM.intern(":"));
    }

    private final String name;
    private final String defaultValue;

    @SuppressWarnings("WeakerAccess")
    public GetPropertyAction(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public GetPropertyAction(String name) {
        this(name, null);
    }

    @Override
    public String run() {
        String value = systemValues.get(name);
        return (value != null) ? value : defaultValue;
    }
}
