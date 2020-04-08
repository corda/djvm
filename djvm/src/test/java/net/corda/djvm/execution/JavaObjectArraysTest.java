package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JavaObjectArraysTest extends TestBase {
    JavaObjectArraysTest() {
        super(JAVA);
    }

    @Test
    void testSandboxingMultiDimensionalArray() {
        sandbox(ctx -> {
            try {
                String[][][] input = {
                    { { "ONE", "TWO" }, { "THREE", "FOUR" } },
                    { { "FIVE", "SIX" }, { "SEVEN", "EIGHT" } }
                };
                Object result = ctx.getClassLoader()
                    .createBasicInput()
                    .apply(input);
                assertNotNull(result);

                Object[][][] objArray = (Object[][][]) result;
                assertArrayOfArray("[[[Lsandbox.java.lang.String;", objArray.getClass());
                assertArrayOfArray("[[Lsandbox.java.lang.String;", objArray[0].getClass());
                assertArrayOfArray("[Lsandbox.java.lang.String;", objArray[0][0].getClass());
                assertArrayOfClass("sandbox.java.lang.String", objArray[0][0][0].getClass());

                DJVM djvm = new DJVM(ctx.getClassLoader());
                List<Object> elements = Arrays.stream(objArray)
                    .flatMap(Arrays::stream)
                    .flatMap(Arrays::stream)
                    .collect(toList());
                assertThat(elements).containsExactly(
                    djvm.stringOf("ONE"),
                    djvm.stringOf("TWO"),
                    djvm.stringOf("THREE"),
                    djvm.stringOf("FOUR"),
                    djvm.stringOf("FIVE"),
                    djvm.stringOf("SIX"),
                    djvm.stringOf("SEVEN"),
                    djvm.stringOf("EIGHT")
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSandboxingHeterogeneousArrays() {
        sandbox(ctx -> {
            try {
                Serializable[][] input = {
                    new Long[] { 1000L, 2000L },
                    new Double[] { 1234.5678d, 5678.9999 }
                };
                Object result = ctx.getClassLoader()
                    .createBasicInput()
                    .apply(input);
                assertNotNull(result);

                Object[][] objArray = (Object[][]) result;
                assertArrayOfArray("[[Ljava.io.Serializable;", objArray.getClass());
                assertArrayOfArray("[Lsandbox.java.lang.Long;", objArray[0].getClass());
                assertArrayOfClass("sandbox.java.lang.Long", objArray[0][0].getClass());
                assertArrayOfArray("[Lsandbox.java.lang.Double;", objArray[1].getClass());
                assertArrayOfClass("sandbox.java.lang.Double", objArray[1][0].getClass());

                DJVM djvm = new DJVM(ctx.getClassLoader());
                List<Object> elements = Arrays.stream(objArray)
                    .flatMap(Arrays::stream)
                    .collect(toList());
                assertThat(elements).containsExactly(
                    djvm.longOf(1000), djvm.longOf(2000),
                    djvm.doubleOf(1234.5678), djvm.doubleOf(5678.9999)
                );
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

                Object[][][] objArray = (Object[][][]) result;
                assertArrayOfArray("[[[Ljava.lang.String;", objArray.getClass());
                assertArrayOfArray("[[Ljava.lang.String;", objArray[0].getClass());
                assertArrayOfArray("[Ljava.lang.String;", objArray[0][0].getClass());
                assertArrayOfClass("java.lang.String", objArray[0][0][0].getClass());

                List<String> elements = Arrays.stream((String[][][])result)
                    .flatMap(Arrays::stream)
                    .flatMap(Arrays::stream)
                    .collect(toList());
                assertThat(elements).containsExactly(
                    "AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "GGG", "HHH"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class CreateMultiArray implements Function<Object, Object> {
        @Override
        public Object apply(Object unused) {
            return new String[][][]{
                { { "AAA", "BBB" }, { "CCC", "DDD" } },
                { { "EEE", "FFF" }, { "GGG", "HHH" } }
            };
        }
    }

    @Test
    void testUnsandboxingHeterogeneousArrays() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object result = WithJava.run(taskFactory, CreateHeterogeneousArray.class, null);
                assertNotNull(result);

                Object[][] objArray = (Object[][]) result;
                assertArrayOfArray("[[Ljava.io.Serializable;", objArray.getClass());
                assertArrayOfArray("[Ljava.lang.String;", objArray[0].getClass());
                assertArrayOfClass("java.lang.String", objArray[0][0].getClass());
                assertArrayOfArray("[Ljava.lang.Integer;", objArray[1].getClass());
                assertArrayOfClass("java.lang.Integer", objArray[1][0].getClass());

                List<Serializable> elements = Arrays.stream((Serializable[][])result)
                    .flatMap(Arrays::stream)
                    .collect(toList());
                assertThat(elements).containsExactly(
                    "Hello", "World", 1234, 5678
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class CreateHeterogeneousArray implements Function<Object, Object> {
        @Override
        public Object apply(Object unused) {
            return new Serializable[][]{
                new String[] { "Hello", "World" },
                new Integer[] { 1234, 5678 }
            };
        }
    }

    private static void assertArrayOfArray(String expectedTypeName, Class<?> type) {
        assertTrue(type.isArray());
        assertEquals(expectedTypeName, type.getName());
    }

    private static void assertArrayOfClass(String expectedTypeName, Class<?> type) {
        assertFalse(type.isArray());
        assertEquals(expectedTypeName, type.getName());
    }
}
