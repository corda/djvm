package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaConstructorAnnotationsTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaConstructorAnnotationsTest.class);
    private static final String GET_CONSTRUCTOR_PARAMETER_ANNOTATIONS = "com.example.testing.GetConstructorParameterAnnotations";
    private static final String GET_ANNOTATIONS_OF_CONSTRUCTOR_PARAMETER = "com.example.testing.GetAnnotationsOfConstructorParameter";

    @Test
    void testConstructorParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] result = WithJava.run(classLoader, GET_CONSTRUCTOR_PARAMETER_ANNOTATIONS, null);
                assertThat(result).hasDimensions(1, 1);
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
                String[][] result = WithJava.run(classLoader, GET_ANNOTATIONS_OF_CONSTRUCTOR_PARAMETER, null);
                assertThat(result).hasDimensions(1, 1);
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
