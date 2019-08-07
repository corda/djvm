package sandbox.java.time.zone;

import sandbox.java.io.DataInputStream;
import sandbox.java.util.Map;
import sandbox.java.util.concurrent.ConcurrentHashMap;

/**
 * This is a dummy class that implements just enough of {@link TzdbZoneRulesProvider}
 * to allow us to compile {@link sandbox.java.time.zone.ZoneRulesProvider}.
 */
@SuppressWarnings({"unused", "RedundantThrows"})
final class TzdbZoneRulesProvider extends ZoneRulesProvider {

    private final Map<String, Object> regionToRules = new ConcurrentHashMap<>();

    TzdbZoneRulesProvider(DataInputStream dis) throws Exception {
        load(dis);
    }

    private void load(DataInputStream dis) throws Exception {
        throw new UnsupportedOperationException("Dummy class - not implemented");
    }
}
