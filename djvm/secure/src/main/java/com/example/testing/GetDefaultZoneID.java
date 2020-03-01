package com.example.testing;

import java.time.ZoneId;
import java.util.function.Function;

public class GetDefaultZoneID implements Function<Object, String> {
    @Override
    public String apply(Object obj) {
        return ZoneId.systemDefault().getId();
    }
}
