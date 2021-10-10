package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaMethodAnnotationsTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaMethodAnnotationsTest.class);
    private static final String GET_JAVA_METHOD_PARAMETER_ANNOTATIONS = "com.example.testing.GetJavaMethodParameterAnnotations";
    private static final String GET_ANNOTATIONS_OF_METHOD_PARAMETER = "com.example.testing.GetAnnotationsOfMethodParameter";

    @Test
    void testMethodParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] result = WithJava.run(classLoader, GET_JAVA_METHOD_PARAMETER_ANNOTATIONS, "action");
                assertThat(result).hasDimensions(1, 1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Big Number))" +
                    "])"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testAnnotationsOfMethodParameterInsideSandbox() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] result = WithJava.run(classLoader, GET_ANNOTATIONS_OF_METHOD_PARAMETER, "action");
                assertThat(result).hasDimensions(1, 1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Big Number))" +
                    "])"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }
}
