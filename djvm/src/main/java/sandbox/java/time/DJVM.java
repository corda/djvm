package sandbox.java.time;

import java.lang.reflect.Method;

public final class DJVM {
    private static final Method createZonedDateTime;

    static {
        try {
            createZonedDateTime = java.time.ZonedDateTime.class.getDeclaredMethod(
                "ofLenient",
                java.time.LocalDateTime.class,
                java.time.ZoneOffset.class,
                java.time.ZoneId.class
            );
            createZonedDateTime.setAccessible(true);
        } catch (Exception e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    private DJVM() {}

    static java.time.ZonedDateTime zonedDateTime(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        try {
            //noinspection JavaReflectionInvocation
            return (java.time.ZonedDateTime) createZonedDateTime.invoke(null, localDateTime.fromDJVM(), offset.fromDJVM(), zone.fromDJVM());
        } catch (Exception e) {
            throw new InternalError(e.getMessage(), e);
        }
    }
}
