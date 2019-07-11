package com.example.testing;

import java.util.function.Function;

public class JavaTask implements Function<String, String> {
    @Override
    public String apply(String input) {
        return String.join("", "Sandbox says: '", input, "'");
    }
}
