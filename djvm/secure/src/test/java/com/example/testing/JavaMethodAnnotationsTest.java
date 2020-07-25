package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaMethodAnnotationsTest extends TestBase {
    @Test
    void testMethodParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetJavaMethodParameterAnnotations.class, "action");
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Big Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testAnnotationsOfMethodParameterInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetAnnotationsOfMethodParameter.class, "action");
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Big Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
