package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoadingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
class DynamicInvocationTest extends TestBase {
    DynamicInvocationTest() {
        super(JAVA);
    }

    @Test
    void testDynamicInvocationDisallowed() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(SandboxClassLoadingException.class, () -> WithJava.run(executor, WithLambda.class, "Hello World"));
            assertThat(ex)
                .isExactlyInstanceOf(SandboxClassLoadingException.class)
                .hasMessageContaining("Disallowed dynamic invocation in " + getClass().getName())
                .hasNoCause();
            return null;
        });
    }

    public static class WithLambda implements Function<String, String> {
        private final Function<String, String> factory = s -> "Sandbox says: '" + s + '\'';

        @Override
        public String apply(String s) {
            return factory.apply(s);
        }
    }
}

