@file:JvmName("RuntimeCostAccounter")
@file:Suppress("unused")
package sandbox

import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.code.impl.OBJECT_NAME
import net.corda.djvm.code.impl.SANDBOX_OBJECT_NAME
import net.corda.djvm.costing.RuntimeCostSummary

/**
 * Class for keeping a tally on various runtime metrics, like number of jumps, allocations, invocations, etc. The
 * functionality is implemented in [RuntimeCostSummary] but proxied through to this class so that the methods can be
 * accessed statically from within the sandbox.
 *
 * Note that the accounter also has thresholds for the various metrics that it is keeping track of, and that it will
 * terminate any sandboxed functions that breach these constraints.
 */

/**
 * The field from the sandbox context which is used to keep track of the costs.
 */
private val runtimeCosts: RuntimeCostSummary = SandboxRuntimeContext.instance.runtimeCosts

/**
 * Known / estimated allocation costs.
 */
private val allocationCosts = mapOf(
        OBJECT_NAME to 8,
        SANDBOX_OBJECT_NAME to 8
)

/**
 * Record a jump operation.
 */
fun recordJump() = runtimeCosts.jumpCost.increment()

/**
 * Record a memory allocation operation.
 *
 * @param typeName The class name of the object being instantiated.
 */
fun recordAllocation(typeName: String) {
    // TODO Derive better size estimates for complex types.
    // Resources worth taking a look at:
    // - https://github.com/DimitrisAndreou/memory-measurer
    // - https://stackoverflow.com/questions/9368764/calculate-size-of-object-in-java
    val size = allocationCosts.getOrDefault(typeName, 16)
    runtimeCosts.allocationCost.increment(size)
}

/**
 * Record an array allocation operation.
 *
 * @param length The number of elements in the array.
 * @param typeName The class name of the array element type.
 */
fun recordArrayAllocation(length: Int, typeName: String) {
    require(length >= 0) { "Length must be a positive integer" }
    val size = allocationCosts.getOrDefault(typeName, 16)
    runtimeCosts.allocationCost.increment(length.toLong() * size)
}

/**
 * Record an array allocation operation.
 *
 * @param length The number of elements in the array.
 * @param typeSize The size of the array element type.
 */
fun recordArrayAllocation(length: Int, typeSize: Int) {
    require(length >= 0) { "Length must be a positive integer" }
    require(typeSize > 0) { "Type size must be a positive integer" }
    runtimeCosts.allocationCost.increment(length.toLong() * typeSize)
}

/**
 * Record a method call.
 */
fun recordInvocation() = runtimeCosts.invocationCost.increment()

/**
 * The accumulated cost of exception throws that have been made.
 */
fun recordThrow() = runtimeCosts.throwCost.increment()
