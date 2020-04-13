package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JavaAnnotationsTest extends TestBase {
    @Test
    void testCheckingForAnnotation() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<?, Boolean> checkAnnotation = taskFactory.create(CheckAnnotationPresent.class);
                assertTrue(checkAnnotation.apply(null));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testGetSingleJavaAnnotation() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<?, String> getAnnotation = taskFactory.create(GetJavaAnnotation.class);
                assertThat(getAnnotation.apply(null)).isEqualTo(
                    "@sandbox.com.example.testing.JavaAnnotations(value=[" +
                        "@sandbox.com.example.testing.JavaAnnotation(value=ONE)" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testGettingAnnotationsByType() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<?, String[][]> getAnnotationsByType = taskFactory.create(GetJavaAnnotationsByType.class);
                String[][] annotations = getAnnotationsByType.apply(null);
                assertThat(annotations).hasSize(3);
                assertThat(annotations[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotation(value=THREE)",
                    "@sandbox.com.example.testing.JavaAnnotation(value=FOUR)",
                    "@sandbox.com.example.testing.JavaAnnotation(value=FIVE)"
                );
                assertThat(annotations[1]).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotation(value=ONE)",
                    "@sandbox.com.example.testing.JavaAnnotation(value=TWO)"
                );
                assertThat(annotations[2]).isEmpty();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testGettingAllAnnotations() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<?, String[]> getAllAnnotations = taskFactory.create(GetAllJavaAnnotations.class);
                String[] annotations = getAllAnnotations.apply(null);
                assertThat(annotations).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotations(value=[" +
                        "@sandbox.com.example.testing.JavaAnnotation(value=ZERO)" +
                    "])",
                    "@sandbox.com.example.testing.JavaTag(value=Simple)"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testDeclaredMethodAnnotations() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<?, String[]> getAnnotations = taskFactory.create(GetJavaMethodAnnotations.class);
                String[] annotations = getAnnotations.apply(null);
                assertThat(annotations).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotation(value=ONE)",
                    "@sandbox.com.example.testing.JavaTag(value=Madness)"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
