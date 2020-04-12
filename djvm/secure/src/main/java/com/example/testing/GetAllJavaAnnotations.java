package com.example.testing;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;

public class GetAllJavaAnnotations implements Function<String, String[]> {
    @Override
    public String[] apply(String unused) {
        Annotation[] annotations = SimpleUserData.class.getAnnotations();
        return Arrays.stream(annotations)
            .map(Annotation::toString)
            .toArray(String[]::new);
    }
}

@JavaAnnotations(@JavaAnnotation)
@JavaTag("Simple")
class SimpleUserData {}
