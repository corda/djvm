package com.example.testing;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;

public class GetConstructorParameterAnnotations implements Function<String, String[][]> {
    @Override
    public String[][] apply(String unused) {
        Annotation[][] annotations;
        try {
            annotations = UserConstructorClass.class.getConstructor(Double.TYPE).getParameterAnnotations();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return Arrays.stream(annotations)
            .map(AnnotationUtils::toStringArray)
            .toArray(String[][]::new);
    }
}
