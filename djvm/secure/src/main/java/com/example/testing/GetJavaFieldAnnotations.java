package com.example.testing;

import java.lang.reflect.Field;
import java.util.function.Function;

import static com.example.testing.AnnotationUtils.toStringArray;

public class GetJavaFieldAnnotations implements Function<String, String[]> {
    @Override
    public String[] apply(String fieldName) {
        Field element;
        try {
            element = UserFieldData.class.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return toStringArray(element.getDeclaredAnnotations());
    }
}

@SuppressWarnings("unused")
class UserFieldData {
    @JavaAnnotation(Label.TWO)
    @JavaTag("Lunacy")
    public String data;
}
