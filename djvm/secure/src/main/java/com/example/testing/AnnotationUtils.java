package com.example.testing;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
final class AnnotationUtils {
    private AnnotationUtils() {}

    static Stream<String> toStrings(Annotation[] annotations) {
        return Arrays.stream(annotations).map(Annotation::toString);
    }

    @NotNull
    static String[] toStringArray(Annotation[] annotations) {
        return toStrings(annotations).toArray(String[]::new);
    }
}
