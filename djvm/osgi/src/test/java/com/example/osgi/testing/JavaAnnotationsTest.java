package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JavaAnnotationsTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaAnnotationsTest.class);

    @Test
    void testCheckingForAnnotation() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Boolean result = WithJava.run(classLoader, "com.example.testing.CheckAnnotationPresent", null);
                assertTrue(result);
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testGetSingleJavaAnnotation() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String annotation = WithJava.run(classLoader, "com.example.testing.GetJavaAnnotation", null);
                assertThat(annotation).isEqualTo(
                    "@sandbox.com.example.testing.JavaAnnotations(value=[" +
                        "@sandbox.com.example.testing.JavaAnnotation(value=ONE)" +
                    "])"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testGettingAnnotationsByType() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] annotations = WithJava.run(classLoader, "com.example.testing.GetJavaAnnotationsByType", null);
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
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testGettingAllAnnotations() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[] annotations = WithJava.run(classLoader, "com.example.testing.GetAllJavaAnnotations", null);
                assertThat(annotations).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotations(value=[" +
                        "@sandbox.com.example.testing.JavaAnnotation(value=ZERO)" +
                    "])",
                    "@sandbox.com.example.testing.JavaTag(value=Simple)"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testJavaMethodAnnotations() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[] annotations = WithJava.run(classLoader, "com.example.testing.GetJavaMethodAnnotations", "action");
                assertThat(annotations).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotation(value=ONE)",
                    "@sandbox.com.example.testing.JavaTag(value=Madness)"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testJavaFieldAnnotations() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[] annotations = WithJava.run(classLoader, "com.example.testing.GetJavaFieldAnnotations", "data");
                assertThat(annotations).containsExactly(
                    "@sandbox.com.example.testing.JavaAnnotation(value=TWO)",
                    "@sandbox.com.example.testing.JavaTag(value=Lunacy)"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }
}
