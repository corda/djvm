package net.corda.djvm.costing

import net.corda.djvm.execution.ExecutionProfile
import java.util.function.Function

/**
 * This class provides a summary of the accumulated costs for the runtime metrics that are being tracked. It also keeps
 * track of applicable thresholds and will terminate sandbox execution if any of them are breached.
 *
 * The costs are tracked on a per-thread basis, and thus, are isolated for each sandbox. Each sandbox live on its own
 * thread.
 */
class RuntimeCostSummary private constructor(
    allocationCostThreshold: Long,
    jumpCostThreshold: Long,
    invocationCostThreshold: Long,
    throwCostThreshold: Long
) {

    /**
     * Create a new runtime cost tracker based on an execution profile.
     */
    constructor(profile: ExecutionProfile) : this(
        allocationCostThreshold = profile.allocationCostThreshold,
        jumpCostThreshold = profile.jumpCostThreshold,
        invocationCostThreshold = profile.invocationCostThreshold,
        throwCostThreshold = profile.throwCostThreshold
    )

    /**
     * Accumulated cost of memory allocations.
     */
    val allocationCost = RuntimeCost(allocationCostThreshold, Function {
        "Sandbox [${it.name}] terminated due to over-allocation"
    })

    /**
     * Accumulated cost of jump operations.
     */
    val jumpCost = RuntimeCost(jumpCostThreshold, Function {
        "Sandbox [${it.name}] terminated due to excessive use of looping"
    })

    /**
     * Accumulated cost of method invocations.
     */
    val invocationCost = RuntimeCost(invocationCostThreshold, Function {
        "Sandbox [${it.name}] terminated due to excessive method calling"
    })

    /**
     * Accumulated cost of throw operations.
     */
    val throwCost = RuntimeCost(throwCostThreshold, Function {
        "Sandbox [${it.name}] terminated due to excessive exception throwing"
    })

}
