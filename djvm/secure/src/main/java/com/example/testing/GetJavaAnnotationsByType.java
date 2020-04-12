package com.example.testing;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;

import static com.example.testing.Label.*;

public class GetJavaAnnotationsByType implements Function<String, String[][]> {
    private String[] toString(JavaAnnotation[] annotations) {
        return Arrays.stream(annotations)
            .map(Annotation::toString)
            .toArray(String[]::new);
    }

    @Override
    public String[][] apply(String unused) {
        JavaAnnotation[] childAnnotations = UserChildData.class.getAnnotationsByType(JavaAnnotation.class);
        JavaAnnotation[] emptyAnnotations = EmptyChildData.class.getAnnotationsByType(JavaAnnotation.class);
        JavaAnnotation[] declaredAnnotations = EmptyChildData.class.getDeclaredAnnotationsByType(JavaAnnotation.class);
        return new String[][]{
            toString(childAnnotations),
            toString(emptyAnnotations),
            toString(declaredAnnotations)
        };
    }
}

@JavaAnnotation(ONE)
@JavaAnnotation(TWO)
class UserBaseData {}

@JavaAnnotation(THREE)
@JavaAnnotation(FOUR)
@JavaAnnotation(FIVE)
class UserChildData extends UserBaseData {}

class EmptyChildData extends UserBaseData {}