package net.corda.djvm.costing

import java.util.function.Function
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * Cost metric to be used in a sandbox environment. The metric has a threshold and a mechanism for reporting violations.
 * The implementation assumes that each metric is tracked on a per-thread basis, i.e., that each sandbox runs on its own
 * thread.
 *
 * @param threshold The threshold for this metric.
 * @param errorMessage A delegate for generating an error message based on the thread it was reported from.
 */
class RuntimeCost(
        threshold: Long,
        errorMessage: Function<Thread, String>
) : TypedRuntimeCost<Long>(
        0,
        Predicate { it > threshold },
        errorMessage
) {
    /**
     * Increment the accumulated cost by an integer.
     */
    fun increment(incrementBy: Int) = increment(incrementBy.toLong())

    /**
     * Increment the accumulated cost by a long integer.
     */
    fun increment(incrementBy: Long) = incrementAndCheck(UnaryOperator { value ->
        Math.addExact(value, incrementBy)
    })

    /**
     * Increment the accumulated cost by one.
     */
    fun increment() = increment(1L)
}
