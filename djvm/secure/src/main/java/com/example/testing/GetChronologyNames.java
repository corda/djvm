package com.example.testing;

import java.time.chrono.Chronology;
import java.util.function.Function;

public class GetChronologyNames implements Function<String, String[]> {
    @Override
    public String[] apply(String s) {
        return Chronology.getAvailableChronologies().stream()
            .map(Chronology::getId)
            .toArray(String[]::new);
    }
}
