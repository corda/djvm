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

class AnnotationProxyJavaConstructorParameterTest extends TestBase {
    AnnotationProxyJavaConstructorParameterTest() {
        super(JAVA);
    }

    @Test
    void testConstructorParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetConstructorParameterAnnotations.class, null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameters(value=[" +
                        "@sandbox.net.corda.djvm.JavaParameter(value=@sandbox.net.corda.djvm.JavaLabel(name=Huge Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetConstructorParameterAnnotations implements Function<String, String[][]> {
        @Override
        public String[][] apply(String unused) {
            Annotation[][] annotations;
            try {
                annotations = UserClass.class.getConstructor(Double.TYPE).getParameterAnnotations();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return Arrays.stream(annotations)
                .map(AnnotationUtils::toStringArray)
                .toArray(String[][]::new);
        }
    }

    @Test
    void testAnnotationsOfConstructorParameterInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetAnnotationsOfConstructorParameter.class, null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameters(value=[" +
                        "@sandbox.net.corda.djvm.JavaParameter(value=@sandbox.net.corda.djvm.JavaLabel(name=Huge Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class GetAnnotationsOfConstructorParameter implements Function<String, String[][]> {
        @Override
        public String[][] apply(String unused) {
            Parameter[] parameters;
            try {
                parameters = UserClass.class.getConstructor(Double.TYPE).getParameters();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return Arrays.stream(parameters)
                .map(Parameter::getAnnotations)
                .map(AnnotationUtils::toStringArray)
                .toArray(String[][]::new);
        }
    }

    @SuppressWarnings("unused")
    public static class UserClass {
        private final double data;

        public UserClass(
            @JavaParameters(
                @JavaParameter(@JavaLabel(name = "Huge Number"))
            ) double data) {
            this.data = data;
        }

        public double getData() {
            return data;
        }
    }
}
