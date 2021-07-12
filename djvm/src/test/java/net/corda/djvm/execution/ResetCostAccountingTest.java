package net.corda.djvm.execution;

import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.assertions.AssertionExtensions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ResetCostAccountingTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ResetCostAccountingTest.class);

    ResetCostAccountingTest() {
        super(JAVA);
    }

    @Test
    void testResetCostAccounting() {
        create(context -> {
            final SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<?, String> defaultTimeZoneTask = taskFactory.create(DefaultTimeZone.class);
                    String defaultTimeZone = defaultTimeZoneTask.apply(null);
                    assertThat(defaultTimeZone).isEqualTo("Coordinated Universal Time");
                } catch (Exception e) {
                    fail(e);
                }
            };

            Consumer<SandboxRuntimeContext> showCosts = ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withAllocationCost(cost -> LOG.info("Allocation Cost: {}", cost))
                    .withInvocationCost(cost -> LOG.info("Invocation Cost: {}", cost))
                    .withThrowCost(cost -> LOG.info("Throw Cost: {}", cost))
                    .withJumpCost(cost -> LOG.info("Jump Cost: {}", cost));

            Consumer<SandboxRuntimeContext> showReset = ctx ->
                assertResetContextFor(ctx)
                    .withResetMethodHandles(handles -> LOG.info("Number of Reset Handles: {}", handles.size()));

            LOG.info("CREATE sandbox");
            sandbox(context, operation.andThen(showCosts).andThen(showReset));

            AtomicLong invocationCost = new AtomicLong();
            AtomicLong allocationCost = new AtomicLong();
            AtomicLong throwCost = new AtomicLong();
            AtomicLong jumpCost = new AtomicLong();

            LOG.info("RESET[1] sandbox");
            sandbox(context, operation.andThen(showCosts).andThen(showReset).andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withAllocationCost(allocationCost::set)
                    .withInvocationCost(invocationCost::set)
                    .withThrowCost(throwCost::set)
                    .withJumpCost(jumpCost::set)
            ));

            LOG.info("RESET[2] sandbox");
            sandbox(context, operation.andThen(showCosts).andThen(showReset).andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(allocationCost.get())
                    .hasInvocationCost(invocationCost.get())
                    .hasThrowCost(throwCost.get())
                    .hasJumpCost(jumpCost.get())
            ));

            LOG.info("RESET[3] sandbox");
            sandbox(context, operation.andThen(showCosts).andThen(showReset).andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(allocationCost.get())
                    .hasInvocationCost(invocationCost.get())
                    .hasThrowCost(throwCost.get())
                    .hasJumpCost(jumpCost.get())
            ));
        });
    }

    public static class DefaultTimeZone implements Function<Object, String> {
        @Override
        public String apply(Object o) {
            return TimeZone.getDefault().getDisplayName();
        }
    }
}
