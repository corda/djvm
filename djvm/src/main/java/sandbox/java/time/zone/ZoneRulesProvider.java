package sandbox.java.time.zone;

import sandbox.java.io.DataInputStream;
import sandbox.java.lang.DJVM;
import sandbox.java.lang.String;
import sandbox.java.lang.Throwable;
import sandbox.java.security.AccessController;
import sandbox.java.security.PrivilegedAction;
import sandbox.java.util.ArrayList;
import sandbox.java.util.List;

/**
 * This is a dummy class that implements just enough of {@link java.time.zone.ZoneRulesProvider}
 * to allow us to compile its anonymous inner class.
 */
@SuppressWarnings({"unused", "WeakerAccess", "Convert2Lambda"})
public abstract class ZoneRulesProvider extends sandbox.java.lang.Object {
    static {
        final List<ZoneRulesProvider> loaded = new ArrayList<>();

        /*
         * This anonymous inner class is a NON-dummy class called ZoneRulesProvider$1.
         */
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try (DataInputStream input = DJVM.loadSystemResource("tzdb.dat")) {
                    registerProvider(new TzdbZoneRulesProvider(input));
                    loaded.clear();
                } catch (Exception ex) {
                    Throwable t = DJVM.doCatch(ex);
                    throw (RuntimeException) DJVM.fromDJVM(new ZoneRulesException(String.toDJVM("Unable to load TZDB time-zone rules"), t));
                }
                return null;
            }
        });
    }

    public static void registerProvider(ZoneRulesProvider provider) {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }
}
