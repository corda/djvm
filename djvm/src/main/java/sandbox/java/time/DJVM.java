package sandbox.java.time;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class DJVM {
    private static final Method createZonedDateTime = AccessController.doPrivileged(new InitAction());

    private static final class InitAction implements PrivilegedAction<Method> {
        @Override
        public Method run() {
            try {
                Method ofLenient = java.time.ZonedDateTime.class.getDeclaredMethod(
                    "ofLenient",
                    java.time.LocalDateTime.class,
                    java.time.ZoneOffset.class,
                    java.time.ZoneId.class
                );
                ofLenient.setAccessible(true);
                return ofLenient;
            } catch (Exception e) {
                throw new InternalError(e.getMessage(), e);
            }
        }
    }

    private DJVM() {}

    static java.time.ZonedDateTime zonedDateTime(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        try {
            return (java.time.ZonedDateTime) createZonedDateTime.invoke(null, localDateTime.fromDJVM(), offset.fromDJVM(), zone.fromDJVM());
        } catch (Exception e) {
            throw new InternalError(e.getMessage(), e);
        }
    }
}
