package com.example.testing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

public class GetJavaMethodAnnotations implements Function<String, String[]> {
    @Override
    public String[] apply(String s) {
        Method action;
        try {
            action = UserMethodData.class.getMethod("action");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return Arrays.stream(action.getDeclaredAnnotations())
            .map(Annotation::toString)
            .toArray(String[]::new);
    }
}

@SuppressWarnings({"unused", "WeakerAccess"})
class UserMethodData {
    @JavaAnnotation(Label.ONE)
    @JavaTag("Madness")
    public void action() {}
}
