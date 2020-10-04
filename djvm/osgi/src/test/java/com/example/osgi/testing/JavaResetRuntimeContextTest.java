package com.example.osgi.testing;

import net.corda.djvm.SandboxRuntimeContext;
import net.corda.djvm.costing.RuntimeCostSummary;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class JavaResetRuntimeContextTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaResetRuntimeContextTest.class);
    private static final String GET_STATIC_HASH_CODE = "com.example.testing.GetStaticHashCode";
    private static final String GET_HASH_CODE = "com.example.testing.GetHashCode";

    @Test
    void testHashCodesSurviveReset() {
        create(context -> {
            SandboxClassLoader classLoader = context.getClassLoader();
            Consumer<SandboxRuntimeContext> operation = ctx -> {
                try {
                    Function<? super Object, Integer> getStaticHashCode = WithJava.create(classLoader, GET_STATIC_HASH_CODE);
                    Function<? super Object, Integer> getHashCode = WithJava.create(classLoader, GET_HASH_CODE);
                    ctx.ready();

                    final int staticHashCode = getStaticHashCode.apply(null);
                    final int hashCode0 = getHashCode.apply("<A>");
                    final int hashCode1 = getHashCode.apply("<B>");
                    assertThat(asList(staticHashCode, hashCode0, hashCode1))
                        .containsExactly(0xfed_c0de - 1, 0xfed_c0de + 1, 0xfed_c0de + 2);
                } catch (Exception e) {
                    LOG.error("Failed", e);
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
