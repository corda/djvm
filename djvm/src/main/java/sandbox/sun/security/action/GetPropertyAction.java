package sandbox.sun.security.action;

import sandbox.java.lang.String;
import sandbox.java.security.PrivilegedAction;

@SuppressWarnings("unused")
public class GetPropertyAction extends sandbox.java.lang.Object implements PrivilegedAction<String> {
    private final String defaultValue;

    @SuppressWarnings("WeakerAccess")
    public GetPropertyAction(String name, String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public GetPropertyAction(String name) {
        this(name, String.toDJVM(""));
    }

    @Override
    public String run() {
        return defaultValue;
    }
}
