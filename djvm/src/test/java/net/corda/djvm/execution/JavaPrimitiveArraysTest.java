package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class JavaPrimitiveArraysTest extends TestBase {
    JavaPrimitiveArraysTest() {
        super(JAVA);
    }

    @Test
    void testSandboxingMultiDimensionalArray() {
        sandbox(ctx -> {
            try {
                int[][][] input = {
                    { { 1, 2 }, { 3, 4 } },
                    { { 5, 6 }, { 7, 8 } }
                };
                Object result = ctx.getClassLoader()
                    .createBasicInput()
                    .apply(input);
                assertNotNull(result);
                assertThat(result).isInstanceOf(int[][][].class);

                int[] elements = Arrays.stream((int[][][]) result)
                    .flatMap(Arrays::stream)
                    .flatMapToInt(Arrays::stream)
                    .toArray();
                assertThat(elements).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testUnsandboxingMultiDimensionalArray() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory, CreateMultiArray.class, null);
                assertNotNull(result);
                assertThat(result).isInstanceOf(int[][][].class);

                int[] elements = Arrays.stream((int[][][]) result)
                    .flatMap(Arrays::stream)
                    .flatMapToInt(Arrays::stream)
                    .toArray();
                assertThat(elements).containsExactly(
                    100, 200, 300, 400, 500, 600, 700, 800
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class CreateMultiArray implements Function<Object, Object> {
        @Override
        public Object apply(Object unused) {
            return new int[][][]{
                { { 100, 200 }, { 300, 400 } },
                { { 500, 600 }, { 700, 800 } }
            };
        }
    }
}