package net.corda.djvm.execution;

import net.corda.djvm.AnnotationUtils;
import net.corda.djvm.JavaLabel;
import net.corda.djvm.JavaParameter;
import net.corda.djvm.JavaParameters;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotationProxyJavaMethodParameterTest extends TestBase {
    AnnotationProxyJavaMethodParameterTest() {
        super(JAVA);
    }

    @Test
    void testMethodParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetMethodParameterAnnotations.class, null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameters(value=[" +
                        "@sandbox.net.corda.djvm.JavaParameter(value=@sandbox.net.corda.djvm.JavaLabel(name=Big Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetMethodParameterAnnotations implements Function<String, String[][]> {
        @Override
        public String[][] apply(String unused) {
            Annotation[][] annotations;
            try {
                annotations = UserClass.class.getMethod("action", Long.TYPE).getParameterAnnotations();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return Arrays.stream(annotations)
                .map(AnnotationUtils::toStringArray)
                .toArray(String[][]::new);
        }
    }

    @Test
    void testAnnotationsOfMethodParameterInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetAnnotationsOfMethodParameter.class, null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameters(value=[" +
                        "@sandbox.net.corda.djvm.JavaParameter(value=@sandbox.net.corda.djvm.JavaLabel(name=Big Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotationsOfMethodParameter implements Function<String, String[][]> {
        @Override
        public String[][] apply(String unused) {
            Parameter[] parameters;
            try {
                parameters = UserClass.class.getMethod("action", Long.TYPE).getParameters();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return Arrays.stream(parameters)
                .map(Parameter::getAnnotations)
                .map(AnnotationUtils::toStringArray)
                .toArray(String[][]::new);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class UserClass {
        public void action(
            @JavaParameters(
                @JavaParameter(@JavaLabel(name = "Big Number"))
            ) long data) {
        }
    }
}
