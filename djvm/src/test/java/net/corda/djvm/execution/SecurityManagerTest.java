package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import static net.corda.djvm.messages.Severity.WARNING;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SecurityManagerTest extends TestBase {
    SecurityManagerTest() {
        super(JAVA);
    }

    @Test
    void testReplacingSecurityManager() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> WithJava.run(executor, ReplacingSecurityManager.class, ""));
            assertThat(ex)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("sandbox.java.security.AccessControlException -> access denied")
                .hasNoCause();
            return null;
        });
    }

    public static class ReplacingSecurityManager implements Function<String, String> {
        @Override
        public String apply(String s) {
            System.setSecurityManager(new SecurityManager() {});
            return null;
        }
    }
}
