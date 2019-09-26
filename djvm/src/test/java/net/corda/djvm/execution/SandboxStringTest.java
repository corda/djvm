package net.corda.djvm.execution;

import net.corda.djvm.TestBase;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.joining;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
            SandboxExecutor<String[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<String> success = WithJava.run(executor, JoinIterableStrings.class, inputs);
            assertThat(success.getResult()).isEqualTo("one+two+three");
            return null;
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
            SandboxExecutor<String[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<String> success = WithJava.run(executor, JoinVarargStrings.class, inputs);
            assertThat(success.getResult()).isEqualTo("ONE+TWO+THREE");
            return null;
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
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertThat(WithJava.run(executor, StringConstant.class, "Wibble!").getResult())
                .isEqualTo("Wibble!");
            return null;
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
            SandboxExecutor<String, byte[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable exception = assertThrows(RuntimeException.class, () -> WithJava.run(executor, GetEncoding.class, "Nonsense-101"));
            assertThat(exception)
                .isExactlyInstanceOf(SandboxRuntimeException.class)
                .hasCauseExactlyInstanceOf(UnsupportedEncodingException.class)
                .hasMessage("Nonsense-101");
            return null;
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
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<String> success = WithJava.run(executor, CreateString.class, charsetName);
            assertThat(success.getResult()).isEqualTo(UNICODE_MESSAGE);
            return null;
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
            SandboxExecutor<String, Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertAll(
                () -> assertThat(WithJava.run(executor, CaseInsensitiveCompare.class, "hello world!").getResult())
                        .isEqualTo(0),
                () -> assertThat(WithJava.run(executor, CaseInsensitiveCompare.class, "GOODBYE!").getResult())
                        .isLessThan(0),
                () -> assertThat(WithJava.run(executor, CaseInsensitiveCompare.class, "zzzzz...").getResult())
                        .isGreaterThan(0)
            );
            return null;
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
            SandboxExecutor<String[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertThat(WithJava.run(executor, Concatenate.class, inputs).getResult())
                .isEqualTo("{dog + cat + mouse + squirrel}");
            return null;
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
            SandboxExecutor<String[], String[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertThat(WithJava.run(executor, Sorted.class, inputs).getResult())
                 .containsExactly("CAT", "PIG", "TREE", "WOLF");
            return null;
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
            SandboxExecutor<String[], String[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertThat(WithJava.run(executor, ComplexStream.class, inputs).getResult())
                .containsExactly("ONE", "TWO", "THREE", "FOUR", "FIVE");
            return null;
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
            SandboxExecutor<String[], String[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertThat(WithJava.run(executor, Spliterate.class, inputs).getResult())
                .containsExactlyInAnyOrder("one+two", "three+four");
            return null;
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
}
