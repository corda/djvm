package com.example.testing;

import net.corda.djvm.execution.DeterministicSandboxExecutor;
import net.corda.djvm.execution.ExecutionSummaryWithResult;
import net.corda.djvm.execution.SandboxExecutor;
import org.junit.jupiter.api.Test;

import static com.example.testing.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaSandboxTest extends TestBase {
    private static final long BIG_NUMBER = 1234L;

    JavaSandboxTest() {
        super(JAVA);
    }

    @Test
    void testGoodTask() {
        sandbox(ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, JavaTask.class, "Hello World!");
            assertEquals("Sandbox says: 'Hello World!'", success.getResult());
            return null;
        });
    }

    @Test
    void testBadTask() {
        sandbox(ctx -> {
            SandboxExecutor<Long, Long> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(NoSuchMethodError.class, () -> WithJava.run(executor, BadJavaTask.class, BIG_NUMBER));
            assertThat(ex)
                .isExactlyInstanceOf(NoSuchMethodError.class)
                .hasMessageContaining("sandbox.java.lang.System.currentTimeMillis()")
                .hasMessageFindingMatch("(long sandbox\\.|\\.currentTimeMillis\\(\\)J)+");
            return null;
        });
    }
}
