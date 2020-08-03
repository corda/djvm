package net.corda.djvm.execution;

import net.corda.djvm.JavaLabel;
import net.corda.djvm.JavaParameter;
import net.corda.djvm.TestBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

import static net.corda.djvm.AnnotationUtils.removeQuotes;
import static net.corda.djvm.AnnotationUtils.toStrings;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotatedJavaConstructorParameterTest extends TestBase {
    AnnotatedJavaConstructorParameterTest() {
        super(JAVA);
    }

    @Test
    void testSandboxConstructorParameterAnnotations() throws NoSuchMethodException {
        assertThat(UserJavaClass.class.getConstructor(String.class, String.class).getParameterAnnotations())
            .hasSize(2);

        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaClass.class);
                Class<?> stringClass = toSandboxClass(ctx, "sandbox.java.lang.String");
                Annotation[][] parameterAnnotations = sandboxClass.getConstructor(stringClass, stringClass)
                    .getParameterAnnotations();
                assertThat(parameterAnnotations).hasSize(2);
                assertThat(removeQuotes(toStrings(parameterAnnotations[0]))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=paramA))"
                );
                assertThat(removeQuotes(toStrings(parameterAnnotations[1]))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=paramB))"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSandboxAnnotationsForConstructorParameters() {
        sandbox(ctx -> {
            try {
                 Class<?> sandboxClass = toSandboxClass(ctx, UserJavaClass.class);
                Class<?> stringClass = toSandboxClass(ctx, "sandbox.java.lang.String");
                Parameter[] parameters = sandboxClass.getConstructor(stringClass, stringClass).getParameters();
                assertThat(parameters).hasSize(2);

                assertThat(removeQuotes(toStrings(parameters[0].getAnnotations()))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=paramA))"
                );
                assertThat(removeQuotes(toStrings(parameters[1].getAnnotations()))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=paramB))"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings("unused")
    public static class UserJavaClass {
        private final String prefix;
        private final String suffix;

        public UserJavaClass(
            @NotNull @JavaParameter(@JavaLabel(name = "paramA")) String prefix,
            @Nullable @JavaParameter(@JavaLabel(name = "paramB")) String suffix
        ) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public String toString() {
            return prefix + suffix;
        }
    }
}
