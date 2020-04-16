package com.example.testing;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

public class GetAnnotationsOfMethodParameter implements Function<String, String[][]> {
    @Override
    public String[][] apply(String unused) {
        Parameter[] parameters;
        try {
            parameters = UserMethodClass.class.getMethod("action", Long.TYPE).getParameters();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return Arrays.stream(parameters)
            .map(Parameter::getAnnotations)
            .map(AnnotationUtils::toStringArray)
            .toArray(String[][]::new);
    }
}
