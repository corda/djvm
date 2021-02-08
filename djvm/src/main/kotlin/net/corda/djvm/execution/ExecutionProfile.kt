package net.corda.djvm.execution

import kotlin.Long.Companion.MAX_VALUE

/**
 * The execution profile of a [Function][java.util.function.Function] when run in a sandbox.
 *
 * @property allocationCostThreshold The threshold placed on allocations.
 * @property invocationCostThreshold The threshold placed on invocations.
 * @property jumpCostThreshold The threshold placed on jumps.
 * @property throwCostThreshold The threshold placed on throw statements.
 */
data class ExecutionProfile(
    val allocationCostThreshold: Long,
    val invocationCostThreshold: Long,
    val jumpCostThreshold: Long,
    val throwCostThreshold: Long
) {
    companion object {
        /**
         * Profile with a set of default thresholds.
         */
        @JvmField
        val DEFAULT = ExecutionProfile(
            allocationCostThreshold = 1024 * 1024 * 1024,
            invocationCostThreshold = 1_000_000,
            jumpCostThreshold = 1_000_000,
            throwCostThreshold = 1_000_000
        )

        /**
         * Profile where no limitations have been imposed on the sandbox.
         */
        @JvmField
        val UNLIMITED = ExecutionProfile(
            allocationCostThreshold = MAX_VALUE,
            invocationCostThreshold = MAX_VALUE,
            jumpCostThreshold = MAX_VALUE,
            throwCostThreshold = MAX_VALUE
        )

        /**
         * Profile where throw statements have been disallowed.
         */
        @JvmField
        val DISABLE_THROWS = ExecutionProfile(
            allocationCostThreshold = MAX_VALUE,
            invocationCostThreshold = MAX_VALUE,
            jumpCostThreshold = MAX_VALUE,
            throwCostThreshold = 0
        )

        /**
         * Profile where branching statements have been disallowed.
         */
        @JvmField
        val DISABLE_BRANCHING = ExecutionProfile(
            allocationCostThreshold = MAX_VALUE,
            invocationCostThreshold = MAX_VALUE,
            jumpCostThreshold = 0,
            throwCostThreshold = MAX_VALUE
        )
    }
}
