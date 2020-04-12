package com.example.testing;

import java.util.function.Function;

public class CheckAnnotationPresent implements Function<String, Boolean> {
    @Override
    public Boolean apply(String unused) {
        return CheckData.class.isAnnotationPresent(JavaAnnotation.class);
    }
}

@JavaAnnotation
class CheckData {}
