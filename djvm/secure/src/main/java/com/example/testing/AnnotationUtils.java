package com.example.testing;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Stream;

public final class AnnotationUtils {
    private AnnotationUtils() {}

    public static Stream<String> toStrings(Annotation[] annotations) {
        return Arrays.stream(annotations).map(Annotation::toString);
    }

    public static Stream<String> removeQuotes(@NotNull Stream<String> stream) {
        return stream.map(s -> s.replace("\"", ""));
    }

    @NotNull
    public static String[] toStringArray(Annotation[] annotations) {
        return toStrings(annotations).toArray(String[]::new);
    }
}
