package net.corda.djvm.assertions

import net.corda.djvm.costing.RuntimeCostSummary
import org.assertj.core.api.Assertions.assertThat
import java.util.function.LongConsumer

@Suppress("MemberVisibilityCanBePrivate")
class AssertiveRuntimeCostSummary(private val costs: RuntimeCostSummary) {

    fun areZero() {
        hasAllocationCost(0)
        hasInvocationCost(0)
        hasJumpCost(0)
        hasThrowCost(0)
    }

    fun hasAllocationCost(cost: Long): AssertiveRuntimeCostSummary {
        assertThat(costs.allocationCost.value)
                .`as`("Allocation cost")
                .isEqualTo(cost)
        return this
    }

    fun withAllocationCost(assertion: LongConsumer): AssertiveRuntimeCostSummary {
        assertion.accept(costs.allocationCost.value)
        return this
    }

    fun hasInvocationCost(cost: Long): AssertiveRuntimeCostSummary {
        assertThat(costs.invocationCost.value)
                .`as`("Invocation cost")
                .isEqualTo(cost)
        return this
    }

    fun withInvocationCost(assertion: LongConsumer): AssertiveRuntimeCostSummary {
        assertion.accept(costs.invocationCost.value)
        return this
    }

    fun hasInvocationCostGreaterThanOrEqualTo(cost: Long): AssertiveRuntimeCostSummary {
        assertThat(costs.invocationCost.value)
                .`as`("Invocation cost")
                .isGreaterThanOrEqualTo(cost)
        return this
    }

    fun withJumpCost(assertion: LongConsumer): AssertiveRuntimeCostSummary {
        assertion.accept(costs.jumpCost.value)
        return this
    }

    fun hasJumpCost(cost: Long): AssertiveRuntimeCostSummary {
        assertThat(costs.jumpCost.value)
                .`as`("Jump cost")
                .isEqualTo(cost)
        return this
    }

    fun hasJumpCostGreaterThanOrEqualTo(cost: Long): AssertiveRuntimeCostSummary {
        assertThat(costs.jumpCost.value)
                .`as`("Jump cost")
                .isGreaterThanOrEqualTo(cost)
        return this
    }

    fun hasThrowCost(cost: Long): AssertiveRuntimeCostSummary {
        assertThat(costs.throwCost.value)
                .`as`("Throw cost")
                .isEqualTo(cost)
        return this
    }

    fun withThrowCost(assertion: LongConsumer): AssertiveRuntimeCostSummary {
        assertion.accept(costs.throwCost.value)
        return this
    }
}
