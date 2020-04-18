package com.example.testing;

import java.util.function.Function;

import static com.example.testing.AnnotationUtils.toStringArray;
import static com.example.testing.Label.*;

public class GetJavaAnnotationsByType implements Function<String, String[][]> {
    @Override
    public String[][] apply(String unused) {
        JavaAnnotation[] childAnnotations = UserChildData.class.getAnnotationsByType(JavaAnnotation.class);
        JavaAnnotation[] emptyAnnotations = EmptyChildData.class.getAnnotationsByType(JavaAnnotation.class);
        JavaAnnotation[] declaredAnnotations = EmptyChildData.class.getDeclaredAnnotationsByType(JavaAnnotation.class);
        return new String[][]{
            toStringArray(childAnnotations),
            toStringArray(emptyAnnotations),
            toStringArray(declaredAnnotations)
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