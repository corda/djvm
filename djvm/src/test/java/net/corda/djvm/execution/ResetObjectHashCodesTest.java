package net.corda.djvm.execution;

import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.assertions.AssertionExtensions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ResetObjectHashCodesTest extends TestBase {
    ResetObjectHashCodesTest() {
        super(JAVA);
    }

    @Test
    void testInitialHashCodesDecrease() {
        create(context -> {
            final SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    // Starting with a fresh context...
                    assertResetContextFor(ctx)
                        .withHashCodeCount(count -> assertThat(count).isZero());

                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);

                    assertResetContextFor(ctx)
                        .withHashCodeCount(count -> assertThat(count).isZero());

                    final int hashCode0 = getHashCode.apply("<X>");
                    final int hashCode1 = getHashCode.apply("<Y>");
                    assertThat(asList(hashCode0, hashCode1)).containsExactly(0xfed_c0de - 1, 0xfed_c0de - 2);
                    assertResetContextFor(ctx)
                        .withHashCodeCount(count -> assertThat(count).isEqualTo(2));
                } catch (Exception e) {
                    fail(e);
                }
            };

            AtomicLong invocationCost = new AtomicLong();
            AtomicLong allocationCost = new AtomicLong();

            /*
             * Execute the operation twice. No hash
             * codes are being persisted statically.
             */
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withAllocationCost(allocationCost::set)
                    .withInvocationCost(invocationCost::set)
                    .withInvocationCost(cost -> assertThat(cost).isPositive())
            ));
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(allocationCost.get())
                    .hasInvocationCost(invocationCost.get())
            ));
        });
    }

    @Test
    void testHashCodesIncreaseOnceReady() {
        create(context -> {
            final SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    // Starting with a fresh context...
                    assertResetContextFor(ctx)
                        .withHashCodeCount(count -> assertThat(count).isZero());

                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                    ctx.ready();

                    assertResetContextFor(ctx)
                        .withHashCodeCount(count -> assertThat(count).isZero());

                    final int hashCode0 = getHashCode.apply("<A>");
                    final int hashCode1 = getHashCode.apply("<B>");
                    assertThat(asList(hashCode0, hashCode1)).containsExactly(0xfed_c0de + 1, 0xfed_c0de + 2);
                    assertResetContextFor(ctx)
                        .withHashCodeCount(count -> assertThat(count).isEqualTo(2));
                } catch (Exception e) {
                    fail(e);
                }
            };

            AtomicLong invocationCost = new AtomicLong();
            AtomicLong allocationCost = new AtomicLong();

            /*
             * Execute the operation twice. No hash
             * codes are being persisted statically.
             */
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withAllocationCost(allocationCost::set)
                    .withInvocationCost(invocationCost::set)
                    .withInvocationCost(cost -> assertThat(cost).isPositive())
            ));
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(allocationCost.get())
                    .hasInvocationCost(invocationCost.get())
            ));
            sandbox(context, operation.andThen(ctx ->
                assertResetContextFor(ctx)
                    .withHashCodeCount(count -> assertThat(count).isEqualTo(2))
            ));
        });
    }

    public static class GetHashCode implements Function<Object, Integer> {
        @Override
        public Integer apply(@NotNull Object obj) {
            return System.identityHashCode(obj);
        }
    }

    @Test
    void testInitialHashCodesSurviveReset() {
        create(context -> {
            final SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<? super Object, Integer> getStaticHashCode = taskFactory.create(GetStaticHashCode.class);
                    Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                    ctx.ready();

                    final int staticHashCode0 = getStaticHashCode.apply(null);
                    final int hashCode0 = getHashCode.apply("<A>");
                    final int hashCode1 = getHashCode.apply("<B>");
                    assertThat(asList(staticHashCode0, hashCode0, hashCode1))
                        .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);
                } catch (Exception e) {
                    fail(e);
                }
            };

            /*
             * Execute the operation. The GetStaticHashCode
             * class retains a static hash code, which should
             * survive resetting the sandbox.
             */
            sandbox(context, ctx ->
                assertResetContextFor(ctx)
                    .withHashCodeCount(count -> assertThat(count).isZero())
            );

            AtomicLong createInvocationCost = new AtomicLong();

            // Execute the operation for the first time.
            // This is likely to be the most expensive.
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withInvocationCost(createInvocationCost::set)
                    .hasAllocationCost(0)
                    .hasInvocationCost(14)
            ));

            AtomicLong resetInvocationCost = new AtomicLong();
            AtomicLong resetAllocationCost = new AtomicLong();

            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .withAllocationCost(resetAllocationCost::set)
                    .withInvocationCost(resetInvocationCost::set)
                    .withInvocationCost(cost ->
                        assertThat(cost)
                            .isLessThanOrEqualTo(createInvocationCost.get())
                            .isPositive())
            ));
            sandbox(context, operation.andThen(ctx ->
                assertThat(ctx.getRuntimeCosts())
                    .hasAllocationCost(resetAllocationCost.get())
                    .hasInvocationCost(resetInvocationCost.get())
            ));

            sandbox(context, ctx ->
                assertResetContextFor(ctx)
                    .withHashCodeCount(count -> assertThat(count).isEqualTo(1))
            );
        });
    }

    public static class GetStaticHashCode implements Function<Object, Integer> {
        private static final int HASH_CODE = GetStaticHashCode.class.hashCode();

        @Override
        public Integer apply(Object obj) {
            return HASH_CODE;
        }
    }
}
