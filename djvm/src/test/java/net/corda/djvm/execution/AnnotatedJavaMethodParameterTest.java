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

class AnnotatedJavaMethodParameterTest extends TestBase {
    AnnotatedJavaMethodParameterTest() {
        super(JAVA);
    }

    @Test
    void testSandboxMethodParameterAnnotations() throws NoSuchMethodException {
        assertThat(UserJavaClass.class.getMethod("getData", String.class, String.class).getParameterAnnotations())
            .hasSize(2);

        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = loadClass(ctx, UserJavaClass.class.getName()).getType();
                Class<?> stringClass = loadClass(ctx, "sandbox.java.lang.String").getType();
                Annotation[][] parameterAnnotations = sandboxClass.getMethod("getData", stringClass, stringClass)
                    .getParameterAnnotations();
                assertThat(parameterAnnotations).hasSize(2);
                assertThat(removeQuotes(toStrings(parameterAnnotations[0]))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param1))"
                );
                assertThat(removeQuotes(toStrings(parameterAnnotations[1]))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param2))"
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
                Class<?> sandboxClass = loadClass(ctx, UserJavaClass.class.getName()).getType();
                Class<?> stringClass = loadClass(ctx, "sandbox.java.lang.String").getType();
                Parameter[] parameters = sandboxClass.getMethod("getData", stringClass, stringClass).getParameters();
                assertThat(parameters).hasSize(2);

                assertThat(removeQuotes(toStrings(parameters[0].getAnnotations()))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param1))"
                );
                assertThat(removeQuotes(toStrings(parameters[1].getAnnotations()))).containsExactly(
                    "@sandbox.net.corda.djvm.JavaParameter$1DJVM(value=@sandbox.net.corda.djvm.JavaLabel$1DJVM(name=param2))"
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
