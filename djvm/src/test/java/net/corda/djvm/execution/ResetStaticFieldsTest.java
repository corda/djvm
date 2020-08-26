package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.code.impl.Types.CLASS_RESET_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ResetStaticFieldsTest extends TestBase {
    ResetStaticFieldsTest() {
        super(JAVA);
    }

    @Test
    void resetStaticFinalObject() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                Function<String, String[]> hasStaticField = taskFactory.create(HasStaticFinalObject.class);
                ctx.ready();

                final String[] firstResult = hasStaticField.apply("Great Wide World!");
                assertThat(firstResult).containsExactly("Hello Sandbox!");
                final String[] secondResult = hasStaticField.apply(null);
                assertThat(secondResult).containsExactly("Great Wide World!");
                reset(ctx);

                final String[] thirdResult = hasStaticField.apply(null);
                assertThat(thirdResult).containsExactly("Hello Sandbox!");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class HasStaticFinalObject implements Function<String, String[]> {
        private static final String[] MESSAGE = { "Hello Sandbox!" };

        @Override
        public String[] apply(String input) {
            String[] result = MESSAGE.clone();
            MESSAGE[0] = input;
            return result;
        }
    }

    @Test
    void resetStaticObject() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                Function<String, String> update = taskFactory.create(HasStaticObject.class);
                ctx.ready();

                assertNull(update.apply("one"));
                assertEquals("one", update.apply("two"));
                assertEquals("two", update.apply("three"));
                reset(ctx);
                assertNull(update.apply("four"));
                assertEquals("four", update.apply("five"));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class HasStaticObject implements Function<String, String> {
        private static String value;

        @Override
        public String apply(String newValue) {
            String oldValue = value;
            value = newValue;
            return oldValue;
        }
    }

    @Test
    void resetStaticPrimitives() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                Function<Character, Object[]> update = taskFactory.create(HasStaticPrimitives.class);
                ctx.ready();

                final Object[] first = update.apply('?');
                assertThat(first).containsExactly(
                    0L, 0, (short) 0, (byte) 0, '\0', false, 0.0d, 0.0f
                );
                final Object[] second = update.apply('*');
                assertThat(second).containsExactly(
                    1L, 1, (short) 1, (byte) 1, '?', true, Math.PI, 1000.0f
                );
                reset(ctx);

                Object[] third = update.apply('&');
                assertThat(third).containsExactly(
                    0L, 0, (short) 0, (byte) 0, '\0', false, 0.0d, 0.0f
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class HasStaticPrimitives implements Function<Character, Object[]> {
        private static long bigNumber;
        private static int number;
        private static short smallNumber;
        private static byte tinyNumber;
        private static char character;
        private static boolean flag;
        private static double bigRealNumber;
        private static float realNumber;

        @Override
        public Object[] apply(Character c) {
            Object[] result = new Object[] {
                bigNumber,
                number,
                smallNumber,
                tinyNumber,
                character,
                flag,
                bigRealNumber,
                realNumber
            };
            ++bigNumber;
            ++number;
            ++smallNumber;
            ++tinyNumber;
            character = c;
            flag ^= true;
            bigRealNumber = Math.PI;
            realNumber += 1000.0f;
            return result;
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {
        ByteOrder.class,
        Modifier.class,
        Random.class,
        CopyOnWriteArrayList.class
    })
    void testClassWithDeletedInit(Class<?> clazz) {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> sandboxClass = classLoader.toSandboxClass(clazz);
                assertThrows(NoSuchMethodException.class, () -> sandboxClass.getDeclaredMethod(CLASS_RESET_NAME));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = "java.nio.Bits")
    void testPrivateImmutableClass(String className) {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> sandboxClass = classLoader.toSandboxClass(className);
                assertThrows(NoSuchMethodException.class, () -> sandboxClass.getDeclaredMethod(CLASS_RESET_NAME));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(classes = SecurityManager.class)
    void testClassWithRecreatedInit(Class<?> clazz) {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Class<?> sandboxClass = classLoader.toSandboxClass(clazz);
                Method resetMethod = sandboxClass.getDeclaredMethod(CLASS_RESET_NAME);
                resetMethod.setAccessible(true);
                resetMethod.invoke(null);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testWithInitHandlingExceptions() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                final String result = WithJava.run(taskFactory, ThrowingClassInit.class, "myValue");
                assertEquals("myValue=1000", result);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class ThrowingClassInit implements Function<String, String> {
        private static final long VALUE;

        static {
            long value;
            try {
                throw new UnsupportedOperationException("Catch me!");
            } catch (UnsupportedOperationException e) {
                value = 1000;
            }
            VALUE = value;
        }

        @Override
        public String apply(String input) {
            return String.format("%s=%d", input, VALUE);
        }
    }

    @Test
    void testResetInterfaceField() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                Function<String, Integer> addToData = taskFactory.create(AddToData.class);
                Function<String, String> getData = taskFactory.create(GetData.class);

                assertEquals(1, addToData.apply("one"));
                assertEquals(2, addToData.apply("two"));
                assertEquals(3, addToData.apply("three"));
                assertEquals("one,two,three", getData.apply(","));
                reset(ctx);
                assertEquals(1, addToData.apply("four"));
                assertEquals("four", getData.apply(","));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    interface HasData {
        List<String> DATA = new ArrayList<>();
    }

    public static class AddToData implements Function<String, Integer> {
        @Override
        public Integer apply(String input) {
            HasData.DATA.add(input);
            return HasData.DATA.size();
        }
    }

    public static class GetData implements Function<String, String> {
        @Override
        public String apply(String delimiter) {
            return String.join(delimiter, HasData.DATA);
        }
    }
}
