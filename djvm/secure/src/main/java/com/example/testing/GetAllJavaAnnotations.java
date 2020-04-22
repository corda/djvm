package com.example.testing;

import java.util.function.Function;

import static com.example.testing.AnnotationUtils.toStringArray;

public class GetAllJavaAnnotations implements Function<String, String[]> {
    @Override
    public String[] apply(String unused) {
        return toStringArray(SimpleUserData.class.getAnnotations());
    }
}

@JavaAnnotations(@JavaAnnotation)
@JavaTag("Simple")
class SimpleUserData {}
