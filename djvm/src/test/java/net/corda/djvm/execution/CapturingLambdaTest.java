package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CapturingLambdaTest extends TestBase {
    private static final long BIG_NUMBER = 1234L;
    private static final int MULTIPLIER = 100;

    CapturingLambdaTest() {
        super(JAVA);
    }

    @Test
    void testCapturingLambda() {
        sandbox(ctx -> {
            SandboxExecutor<Long, Long> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, CapturingLambda.class, BIG_NUMBER);
            assertEquals(BIG_NUMBER * MULTIPLIER, success.getResult());
            return null;
        });
    }

    public static class CapturingLambda implements Function<Long, Long> {
        private final BigDecimal value = new BigDecimal(MULTIPLIER);

        @Override
        public Long apply(Long input) {
            Function<BigDecimal, BigDecimal> lambda = value::multiply;
            return lambda.apply(new BigDecimal(input)).longValue();
        }
    }
}