package com.example.testing;

import java.util.function.Function;

public class GetStaticHashCode implements Function<Object, Integer> {
    private static final int HASH_CODE = GetStaticHashCode.class.hashCode();

    @Override
    public Integer apply(Object obj) {
        return HASH_CODE;
    }
}
