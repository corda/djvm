package com.example.testing;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class GetHashCode implements Function<Object, Integer> {
    @Override
    public Integer apply(@NotNull Object obj) {
        return System.identityHashCode(obj);
    }
}
