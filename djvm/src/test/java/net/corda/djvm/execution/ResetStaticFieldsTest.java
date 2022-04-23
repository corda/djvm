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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.code.impl.Types.CLASS_RESET_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class ResetStaticFieldsTest extends TestBase {
    private static final String STATIC_DATA = "Hello Sandbox!";
    private static final String TEST_MESSAGE = "Great Wide World!";

    ResetStaticFieldsTest() {
        super(JAVA);
    }

    @Test
    void resetStaticFinalObject() {
        create(context -> {
            SandboxClassLoader classLoader = context.getClassLoader();
            final List<String> internStrings = new LinkedList<>();
            final List<String> resetHandles = new LinkedList<>();
            sandbox(context, ctx -> {
                try {
                    // Starting with a fresh context...
                    assertResetContextFor(ctx)
                        .withInternStrings(strings -> assertThat(strings).isEmpty())
                        .withResetMethodHandles(handles -> assertThat(handles).isEmpty());

                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<String, String[]> hasStaticField = taskFactory.create(HasStaticFinalObject.class);

                    // Check we have acquired our resettable classes and interned strings.
                    assertResetContextFor(ctx)
                        .withInternStrings(internStrings::addAll)
                        .withResetMethodHandles(resetHandles::addAll)
                        .withInternStrings(strings -> assertThat(strings).contains(STATIC_DATA, "", "\n", "true", "false"))
                        .withResetMethodHandles(handles -> assertThat(handles).hasSize(2));

                    // Switch from "setup" phase to "run" phase.
                    ctx.ready();

                    // Check we still have our interned strings, but no resettable classes.
                    assertResetContextFor(ctx)
                        .withInternStrings(strings -> assertThat(strings).isEqualTo(internStrings))
                        .withResetMethodHandles(handles -> assertThat(handles).isEmpty());

                    final String[] firstResult = hasStaticField.apply(TEST_MESSAGE);
                    assertThat(firstResult).containsExactly(STATIC_DATA);
                    final String[] secondResult = hasStaticField.apply(null);
                    assertThat(secondResult).containsExactly(TEST_MESSAGE);

                    // Check that nothing has changed.
                    assertResetContextFor(ctx)
                        .withInternStrings(strings -> assertThat(strings).isEqualTo(internStrings))
                        .withResetMethodHandles(handles -> assertThat(handles).isEmpty());
                } catch (Exception e) {
                    fail(e);
                }
            });

            sandbox(context, ctx -> {
                try {
                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles))
                        .withInternStrings(strings -> assertThat(strings).isEqualTo(internStrings));

                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<String, String[]> hasStaticField = taskFactory.create(HasStaticFinalObject.class);
                    final String[] thirdResult = hasStaticField.apply(null);
                    assertThat(thirdResult).containsExactly(STATIC_DATA);

                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles))
                        .withInternStrings(strings -> assertThat(strings).isEqualTo(internStrings));
                } catch (Exception e) {
                    fail(e);
                }
            });
        });
    }

    public static class HasStaticFinalObject implements Function<String, String[]> {
        private static final String[] MESSAGE = { STATIC_DATA };

        @Override
        public String[] apply(String input) {
            String[] result = MESSAGE.clone();
            MESSAGE[0] = input;
            return result;
        }
    }

    @Test
    void resetStaticObject() {
        create(context -> {
            SandboxClassLoader classLoader = context.getClassLoader();
            final List<String> resetHandles = new LinkedList<>();
            sandbox(context, ctx -> {
                try {
                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).isEmpty());

                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<String, String> update = taskFactory.create(HasStaticObject.class);

                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).hasSize(2))
                        .withResetMethodHandles(resetHandles::addAll);

                    ctx.ready();

                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).isEmpty());

                    assertNull(update.apply("one"));
                    assertEquals("one", update.apply("two"));
                    assertEquals("two", update.apply("three"));

                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).isEmpty());
                } catch (Exception e) {
                    fail(e);
                }
            });

            sandbox(context, ctx -> {
                try {
                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles));

                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<String, String> update = taskFactory.create(HasStaticObject.class);
                    assertNull(update.apply("four"));
                    assertEquals("four", update.apply("five"));

                    assertResetContextFor(ctx)
                        .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles));
                } catch (Exception e) {
                    fail(e);
                }
            });
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
        create(context -> {
            SandboxClassLoader classLoader = context.getClassLoader();
            sandbox(context, ctx -> {
                try {
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
                } catch (Exception e) {
                    fail(e);
                }
            });

            sandbox(context, ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<Character, Object[]> update = taskFactory.create(HasStaticPrimitives.class);
                    Object[] third = update.apply('&');
                    assertThat(third).containsExactly(
                        0L, 0, (short) 0, (byte) 0, '\0', false, 0.0d, 0.0f
                    );
                } catch (Exception e) {
                    fail(e);
                }
            });
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
                assertThrows(NoSuchMethodException.class, () -> sandboxClass.getDeclaredMethod(CLASS_RESET_NAME, BiConsumer.class));
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
                assertThrows(NoSuchMethodException.class, () -> sandboxClass.getDeclaredMethod(CLASS_RESET_NAME, BiConsumer.class));
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
                Method resetMethod = sandboxClass.getDeclaredMethod(CLASS_RESET_NAME, BiConsumer.class);
                resetMethod.setAccessible(true);
                resetMethod.invoke(null, (BiConsumer<Object, String>)(value, name) -> {});
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
        create(context -> {
            SandboxClassLoader classLoader = context.getClassLoader();
            sandbox(context, ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<String, Integer> addToData = taskFactory.create(AddToData.class);
                    Function<String, String> getData = taskFactory.create(GetData.class);

                    assertEquals("", getData.apply(","));
                    assertEquals(1, addToData.apply("one"));
                    assertEquals(2, addToData.apply("two"));
                    assertEquals(3, addToData.apply("three"));
                    assertEquals("one,two,three", getData.apply(","));
                } catch (Exception e) {
                    fail(e);
                }
            });

            sandbox(context, ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<String, Integer> addToData = taskFactory.create(AddToData.class);
                    Function<String, String> getData = taskFactory.create(GetData.class);

                    assertEquals("", getData.apply(","));
                    assertEquals(1, addToData.apply("four"));
                    assertEquals("four", getData.apply(","));
                } catch (Exception e) {
                    fail(e);
                }
            });
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
