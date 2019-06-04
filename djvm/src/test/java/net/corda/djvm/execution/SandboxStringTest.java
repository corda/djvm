package net.corda.djvm.execution;

import net.corda.djvm.TestBase;

import static java.util.Arrays.asList;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.function.Function;

class SandboxStringTest extends TestBase {
    private static final String MESSAGE = "Goodbye, Cruel World! \u1F4A9";

    @Test
    void testJoiningIterableInsideSandbox() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, JoinIterableStrings.class, new String[]{"one", "two", "three"});
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
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, JoinVarargStrings.class, new String[]{"ONE", "TWO", "THREE"});
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
    void encodeStringWithUnknownCharset() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, byte[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable exception = assertThrows(RuntimeException.class, () -> WithJava.run(executor, GetEncoding.class, "Nonsense-101"));
            assertThat(exception)
                .hasCauseExactlyInstanceOf(UnsupportedEncodingException.class)
                .hasMessage("Nonsense-101");
            return null;
        });
   }

    public static class GetEncoding implements Function<String, byte[]> {
        @Override
        public byte[] apply(String charsetName) {
            try {
                return MESSAGE.getBytes(charsetName);
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "UTF-16", "UTF-32"})
    void decodeStringWithCharset(String charsetName) {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, CreateString.class, charsetName);
            assertThat(success.getResult()).isEqualTo(MESSAGE);
            return null;
        });
    }

    public static class CreateString implements Function<String, String> {
        @Override
        public String apply(String charsetName) {
            try {
                return new String(MESSAGE.getBytes(Charset.forName(charsetName)), charsetName);
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
