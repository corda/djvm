package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaConstructorAnnotationsTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaConstructorAnnotationsTest.class);

    @Test
    void testConstructorParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] result = WithJava.run(classLoader, "com.example.testing.GetConstructorParameterAnnotations", null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Huge Number))" +
                    "])"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testAnnotationsOfConstructorParameterInsideSandbox() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] result = WithJava.run(classLoader, "com.example.testing.GetAnnotationsOfConstructorParameter", null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Huge Number))" +
                    "])"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }
}
