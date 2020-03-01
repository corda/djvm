package com.example.testing;

import java.util.TimeZone;
import java.util.function.Function;

public class GetDefaultTimeZone implements Function<Object, String> {
    @Override
    public String apply(Object obj) {
        return TimeZone.getDefault().getDisplayName();
    }
}
