package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaConstructorAnnotationsTest extends TestBase {
    @Test
    void testConstructorParameterAnnotationsInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetConstructorParameterAnnotations.class, null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Huge Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testAnnotationsOfConstructorParameterInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[][] result = WithJava.run(taskFactory, GetAnnotationsOfConstructorParameter.class, null);
                assertThat(result).hasSize(1);
                assertThat(result[0]).containsExactly(
                    "@sandbox.com.example.testing.JavaParameters(value=[" +
                        "@sandbox.com.example.testing.JavaParameter(value=@sandbox.com.example.testing.JavaTag(value=Huge Number))" +
                    "])"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
