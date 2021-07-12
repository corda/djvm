package net.corda.djvm.execution;

import net.corda.djvm.TestBase;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.joining;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class SandboxStringTest extends TestBase {
    private static final String UNICODE_MESSAGE = "Goodbye, Cruel World! \u1F4A9";
    private static final String HELLO_WORLD = "Hello World!";

    SandboxStringTest() {
        super(JAVA);
    }

    @Test
    void testJoiningIterableInsideSandbox() {
        String[] inputs = new String[]{"one", "two", "three"};
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, JoinIterableStrings.class, inputs);
                assertThat(result).isEqualTo("one+two+three");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class JoinIterableStrings implements Function<String[], String> {
        @Override
        public String apply(String[] input) {
            return String.join("+", asList(input));
        }
    }

    @Test
    void testJoiningVarargInsideSandbox() {
        String[] inputs = new String[]{"ONE", "TWO", "THREE"};
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, JoinVarargStrings.class, inputs);
                assertThat(result).isEqualTo("ONE+TWO+THREE");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class JoinVarargStrings implements Function<String[], String> {
        @Override
        public String apply(String[] input) {
            return String.join("+", input);
        }
    }

    @Test
    void testStringConstant() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThat(WithJava.run(taskFactory, StringConstant.class, "Wibble!"))
                        .isEqualTo("Wibble!");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class StringConstant implements Function<String, String> {
        @SuppressWarnings("all")
        @Override
        public String apply(String input) {
            String constant = input.intern();
            if (!constant.equals(input)) {
                throw new IllegalArgumentException("String constant has wrong value: '" + constant + '\'');
            } else if (constant != "Wibble!") {
                throw new IllegalArgumentException("String constant has not been interned");
            }
            return constant;
        }
    }

    @Test
    void encodeStringWithUnknownCharset() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable exception = assertThrows(RuntimeException.class, () -> WithJava.run(taskFactory, GetEncoding.class, "Nonsense-101"));
                assertThat(exception)
                    .isExactlyInstanceOf(SandboxRuntimeException.class)
                    .hasCauseExactlyInstanceOf(UnsupportedEncodingException.class)
                    .hasMessage("Nonsense-101");
            } catch(Exception e) {
                fail(e);
            }
        });
   }

    public static class GetEncoding implements Function<String, byte[]> {
        @Override
        public byte[] apply(String charsetName) {
            try {
                return UNICODE_MESSAGE.getBytes(charsetName);
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "UTF-16", "UTF-32"})
    void decodeStringWithCharset(String charsetName) {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, CreateString.class, charsetName);
                assertThat(result).isEqualTo(UNICODE_MESSAGE);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class CreateString implements Function<String, String> {
        @Override
        public String apply(String charsetName) {
            try {
                return new String(UNICODE_MESSAGE.getBytes(Charset.forName(charsetName)), charsetName);
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    void testCaseInsensitiveComparison() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertAll(
                    () -> assertThat(WithJava.run(taskFactory, CaseInsensitiveCompare.class, "hello world!"))
                            .isEqualTo(0),
                    () -> assertThat(WithJava.run(taskFactory, CaseInsensitiveCompare.class, "GOODBYE!"))
                            .isLessThan(0),
                    () -> assertThat(WithJava.run(taskFactory, CaseInsensitiveCompare.class, "zzzzz..."))
                            .isGreaterThan(0)
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class CaseInsensitiveCompare implements Function<String, Integer> {
        @Override
        public Integer apply(String str) {
            return String.CASE_INSENSITIVE_ORDER.compare(str, HELLO_WORLD);
        }
    }

    @Test
    void testStream() {
        String[] inputs = new String[] {"dog", "cat", "mouse", "squirrel"};
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThat(WithJava.run(taskFactory, Concatenate.class, inputs))
                    .isEqualTo("{dog + cat + mouse + squirrel}");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class Concatenate implements Function<String[], String> {
        @Override
        public String apply(String[] inputs) {
            return stream(inputs).collect(joining(" + ", "{", "}"));
        }
    }

    @Test
    void testSorting() {
        String[] inputs = Stream.of("Wolf", "Cat", "Tree", "Pig").map(String::toUpperCase).toArray(String[]::new);
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThat(WithJava.run(taskFactory, Sorted.class, inputs))
                    .containsExactly("CAT", "PIG", "TREE", "WOLF");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class Sorted implements Function<String[], String[]> {
        @Override
        public String[] apply(String[] inputs) {
            List<String> list = asList(inputs);
            list.sort(null);
            return list.toArray(new String[0]);
        }
    }

    @Test
    void testComplexStream() {
        String[] inputs = new String[] { "one", "two", "three", "four", "five" };
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThat(WithJava.run(taskFactory, ComplexStream.class, inputs))
                    .containsExactly("ONE", "TWO", "THREE", "FOUR", "FIVE");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class ComplexStream implements Function<String[], String[]> {
        @Override
        public String[] apply(String[] inputs) {
            return Stream.of(inputs).map(String::toUpperCase).toArray(String[]::new);
        }
    }

    @Test
    void testSpliterator() {
        String[] inputs = new String[] { "one", "two", "three", "four" };
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThat(WithJava.run(taskFactory, Spliterate.class, inputs))
                    .containsExactlyInAnyOrder("one+two", "three+four");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class Spliterate implements Function<String[], String[]> {
        @Override
        public String[] apply(String[] inputs) {
            Spliterator<String> split1 = asList(inputs).spliterator();
            Spliterator<String> split2 = split1.trySplit();
            return new String[] { join(split1), join(split2) };
        }

        private String join(Spliterator<String> split) {
            return StreamSupport.stream(split, false).collect(joining("+"));
        }
    }

    @Test
    void testObjectToString() {
        UUID uuid = UUID.randomUUID();
        Object[] inputs = new Object[]{
            new String[]{ "TEXT" },
            new byte[]{ 127 },
            new long[]{ 5000 },
            HELLO_WORLD,
            uuid
        };
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] results = WithJava.run(taskFactory, ToStringTask.class, inputs);
                assertThat(results).containsExactlyInAnyOrder(
                    "sandbox.java.lang.Object@fedc0dc",
                    "sandbox.java.lang.Object@fedc0db",
                    "sandbox.java.lang.Object@fedc0da",
                    HELLO_WORLD,
                    uuid.toString()
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class ToStringTask implements Function<Object[], String[]> {
        @Override
        public String[] apply(Object[] inputs) {
            return Arrays.stream(inputs)
                .map(Object::toString)
                .toArray(String[]::new);
        }
    }
}
