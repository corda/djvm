package com.example.testing;

import java.time.zone.ZoneRulesProvider;
import java.util.function.Function;

public class GetAllZoneIDs implements Function<Object, String[]> {
    @Override
    public String[] apply(Object o) {
        return ZoneRulesProvider
            .getAvailableZoneIds()
            .toArray(new String[0]);
    }
}
