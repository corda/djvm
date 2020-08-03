package com.example.testing;

import java.lang.reflect.Method;
import java.util.function.Function;

import static com.example.testing.AnnotationUtils.toStringArray;

public class GetJavaMethodAnnotations implements Function<String, String[]> {
    @Override
    public String[] apply(String methodName) {
        Method action;
        try {
            action = UserMethodData.class.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return toStringArray(action.getDeclaredAnnotations());
    }
}

@SuppressWarnings("unused")
class UserMethodData {
    @JavaAnnotation(Label.ONE)
    @JavaTag("Madness")
    public void action() {}
}
