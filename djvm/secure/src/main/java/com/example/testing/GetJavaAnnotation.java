package com.example.testing;

import java.util.function.Function;

import static com.example.testing.Label.ONE;

public class GetJavaAnnotation implements Function<String, String> {
    @Override
    public String apply(String unused) {
        JavaAnnotations annotation = UserData.class.getAnnotation(JavaAnnotations.class);
        return annotation == null ? null : annotation.toString();
    }
}

@JavaAnnotations(@JavaAnnotation(ONE))
class UserData {}