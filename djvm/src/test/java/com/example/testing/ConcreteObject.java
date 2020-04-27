package com.example.testing;

public class ConcreteObject extends GenericObject<Long, String> {
    @Override
    public String apply(Long input) {
        return input == null ? null : input.toString();
    }
}
