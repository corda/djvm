package net.corda.djvm.execution;

import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.singletonMap;
import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.assertions.AssertionExtensions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ClassResetOrderTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ClassResetOrderTest.class);
    public static final String FOO_LABEL = "Foo";
    public static final String EXTRA_DATA = "Extra!";

    ClassResetOrderTest() {
        super(JAVA);
    }

    @Test
    void testResetClassWithInternalEnum() {
        create(context -> {
            final SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    String result = WithJava.run(taskFactory, ComplexInit.class, "FOO");
                    assertThat(result).isEqualTo(FOO_LABEL + ':' + EXTRA_DATA);
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

            final List<String> resetHandles = new LinkedList<>();

            LOG.info("CREATE sandbox");
            sandbox(context, operation.andThen(showCosts).andThen(showReset).andThen(ctx ->
                assertResetContextFor(ctx)
                    .withResetMethodHandles(resetHandles::addAll)
            ));

            AtomicLong invocationCost = new AtomicLong();
            AtomicLong allocationCost = new AtomicLong();
            AtomicLong throwCost = new AtomicLong();
            AtomicLong jumpCost = new AtomicLong();

            LOG.info("RESET[1] sandbox");
            sandbox(context, operation.andThen(showCosts).andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withAllocationCost(allocationCost::set)
                    .withInvocationCost(invocationCost::set)
                    .withThrowCost(throwCost::set)
                    .withJumpCost(jumpCost::set)
            ).andThen(ctx ->
                assertResetContextFor(ctx)
                    .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles))
            ));

            LOG.info("RESET[2] sandbox");
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(allocationCost.get())
                    .hasInvocationCost(invocationCost.get())
                    .hasThrowCost(throwCost.get())
                    .hasJumpCost(jumpCost.get())
            ).andThen(ctx ->
                assertResetContextFor(ctx)
                    .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles))
            ));
            LOG.info("RESET[3] sandbox");
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(allocationCost.get())
                    .hasInvocationCost(invocationCost.get())
                    .hasThrowCost(throwCost.get())
                    .hasJumpCost(jumpCost.get())
            ).andThen(ctx ->
                assertResetContextFor(ctx)
                    .withResetMethodHandles(handles -> assertThat(handles).hasSameSizeAs(resetHandles))
            ));
        });
    }

    public static class ComplexInit implements Function<String, String> {
        enum Type {
            FOO(FOO_LABEL);

            private final String label;

            Type(String label) {
                this.label = label;
            }

            String getLabel() {
                return label;
            }
        }

        private static final Map<Type, String> CACHE;

        static {
            CACHE = singletonMap(Type.FOO, EXTRA_DATA);
        }

        @Override
        public String apply(String input) {
            Type type = Type.valueOf(input);
            String value = CACHE.get(type);
            return (value == null) ? "FAIL" : type.getLabel() + ':' + value;
        }
    }
}
