package sandbox.sun.util.calendar;

import sandbox.java.io.DataInputStream;
import sandbox.java.lang.DJVM;
import sandbox.java.security.AccessController;
import sandbox.java.security.PrivilegedAction;

import java.io.IOException;

/**
 * This is a dummy class that implements just enough of {@link sun.util.calendar.ZoneInfoFile}
 * to allow us to compile its anonymous inner class.
 */
@SuppressWarnings({"unused", "Convert2Lambda", "RedundantThrows"})
public final class ZoneInfoFile extends sandbox.java.lang.Object {
    static {
        /*
         * This anonymous inner class is a NON-dummy class called ZoneInfoFile$1.
         */
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try (DataInputStream input = DJVM.loadSystemResource("tzdb.dat")) {
                    ZoneInfoFile.load(input);
                } catch (Exception e) {
                    throw new InternalError(e);
                }
                return null;
            }
        });
    }

    private static void load(DataInputStream dis) throws ClassNotFoundException, IOException {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }
}
