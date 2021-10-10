package net.corda.djvm.execution;

import net.corda.djvm.JavaLabel;
import net.corda.djvm.JavaParameter;
import net.corda.djvm.TestBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

import static java.util.regex.Pattern.matches;
import static net.corda.djvm.AnnotationUtils.removeQuotes;
import static net.corda.djvm.AnnotationUtils.toStrings;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class AnnotatedJavaMethodParameterTest extends TestBase {
    AnnotatedJavaMethodParameterTest() {
        super(JAVA);
    }

    @Test
    void testSandboxMethodParameterAnnotations() throws NoSuchMethodException {
        assertThat(UserJavaClass.class.getMethod("getData", String.class, String.class).getParameterAnnotations())
            .hasDimensions(2, 1);

        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaClass.class);
                Class<?> stringClass = toSandboxClass(ctx, "sandbox.java.lang.String");
                Annotation[][] parameterAnnotations = sandboxClass.getMethod("getData", stringClass, stringClass)
                    .getParameterAnnotations();
                assertThat(parameterAnnotations).hasDimensions(2, 1);
                assertThat(removeQuotes(toStrings(parameterAnnotations[0]))).allMatch(s ->
                    matches("\\Q@sandbox.net.corda.djvm.JavaParameter$1DJVM(\\E(value=)?\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param1))\\E", s)
                );
                assertThat(removeQuotes(toStrings(parameterAnnotations[1]))).allMatch(s ->
                    matches("\\Q@sandbox.net.corda.djvm.JavaParameter$1DJVM(\\E(value=)?\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param2))\\E", s)
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSandboxAnnotationsForMethodParameters() {
        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = toSandboxClass(ctx, UserJavaClass.class);
                Class<?> stringClass = toSandboxClass(ctx, "sandbox.java.lang.String");
                Parameter[] parameters = sandboxClass.getMethod("getData", stringClass, stringClass).getParameters();
                assertThat(parameters).hasSize(2);

                assertThat(removeQuotes(toStrings(parameters[0].getAnnotations()))).allMatch(s ->
                    matches("\\Q@sandbox.net.corda.djvm.JavaParameter$1DJVM(\\E(value=)?\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param1))\\E", s)
                );
                assertThat(removeQuotes(toStrings(parameters[1].getAnnotations()))).allMatch(s ->
                    matches("\\Q@sandbox.net.corda.djvm.JavaParameter$1DJVM(\\E(value=)?\\Q@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param2))\\E", s)
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class UserJavaClass {
        public String getData(
            @NotNull @JavaParameter(@JavaLabel(name = "param1")) String prefix,
            @Nullable @JavaParameter(@JavaLabel(name = "param2")) String suffix
        ) {
            return prefix + suffix;
        }
    }
}
