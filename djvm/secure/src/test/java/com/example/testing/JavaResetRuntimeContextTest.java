package com.example.testing;

import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.costing.RuntimeCostSummary;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class JavaResetRuntimeContextTest extends TestBase {
    @Test
    void testHashCodesSurviveReset() {
        create(context -> {
            SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    TypedTaskFactory taskFactory = classLoader.createTypedTaskFactory();
                    Function<? super Object, Integer> getStaticHashCode = taskFactory.create(GetStaticHashCode.class);
                    Function<? super Object, Integer> getHashCode = taskFactory.create(GetHashCode.class);
                    ctx.ready();

                    final int staticHashCode = getStaticHashCode.apply(null);
                    final int hashCode0 = getHashCode.apply("<A>");
                    final int hashCode1 = getHashCode.apply("<B>");
                    assertThat(asList(staticHashCode, hashCode0, hashCode1))
                        .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);
                } catch (Exception e) {
                    fail(e);
                }
            };

            sandbox(context, operation.andThen(ctx -> {
                RuntimeCostSummary costs = ctx.getRuntimeCosts();
                assertEquals(0, costs.getAllocationCost().getValue());
                assertEquals(14, costs.getInvocationCost().getValue());
            }));
            sandbox(context, operation.andThen(ctx -> {
                RuntimeCostSummary costs = ctx.getRuntimeCosts();
                assertEquals(0, costs.getAllocationCost().getValue());
                assertEquals(14, costs.getInvocationCost().getValue());
            }));
        });
    }
}
