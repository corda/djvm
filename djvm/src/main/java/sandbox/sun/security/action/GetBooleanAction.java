package sandbox.sun.security.action;

import sandbox.java.lang.Boolean;
import sandbox.java.lang.String;
import sandbox.java.security.PrivilegedAction;

@SuppressWarnings("unused")
public class GetBooleanAction extends sandbox.java.lang.Object implements PrivilegedAction<Boolean> {
    public GetBooleanAction(String propertyName) {
    }

    @Override
    public Boolean run() {
        return Boolean.FALSE;
    }
}
