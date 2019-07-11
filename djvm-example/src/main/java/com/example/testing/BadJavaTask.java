package com.example.testing;

import java.util.function.Function;

public class BadJavaTask implements Function<Long, Long> {
    @Override
    public Long apply(Long input) {
        return System.currentTimeMillis() + input;
    }
}
