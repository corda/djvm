package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaMethodAnnotationsTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaMethodAnnotationsTest.class);

    @Test
    void testMethodParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[][] result = WithJava.run(classLoader, "com.example.testing.GetJavaMethodParameterAnnotations", "action");
                assertThat(result).hasSize(1);
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
                String[][] result = WithJava.run(classLoader, "com.example.testing.GetAnnotationsOfMethodParameter", "action");
                assertThat(result).hasSize(1);
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
