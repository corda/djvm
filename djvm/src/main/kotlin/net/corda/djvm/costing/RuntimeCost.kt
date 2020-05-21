package net.corda.djvm.costing

import net.corda.djvm.utilities.loggerFor
import java.util.function.Function

/**
 * Cost metric to be used in a sandbox environment. The metric has a threshold and a mechanism for reporting violations.
 * The implementation assumes that each metric is tracked on a per-thread basis, i.e., that each sandbox runs on its own
 * thread.
 *
 * @param threshold The threshold for this metric.
 * @param errorMessage A delegate for generating an error message based on the thread it was reported from.
 */
class RuntimeCost(
    private val threshold: Long,
    private val errorMessage: Function<Thread, String>
) {
    /**
     * The thread-local container for the cost accumulator.
     */
    private val costValue = object : ThreadLocal<Long>() {
        override fun initialValue(): Long = 0
    }

    /**
     * Property getter for accessing the current accumulated cost.
     */
    val value: Long
        get() = costValue.get()

    /**
     * Helper function for doing a guarded increment of the cost value, with a mechanism for consistent error reporting
     * and nuking of the current thread environment if threshold breaches are encountered.
     */
    private fun incrementAndCheck(incrementBy: Long) {
        val currentThread = getAndCheckThread() ?: return
        val newValue = Math.addExact(costValue.get(), incrementBy)
        costValue.set(newValue)
        if (newValue > threshold) {
            val message = errorMessage.apply(currentThread)
            logger.error("Threshold breached; {}", message)
            throw ThresholdViolationError(message)
        }
    }

    /**
     * If [filteredThreads] is specified, this method will filter out those threads whenever threshold constraints are
     * being tested. This can be used to disable cost accounting on a primary thread, for instance.
     */
    private fun getAndCheckThread(): Thread? {
        val currentThread = Thread.currentThread()
        if (currentThread in filteredThreads) {
            logger.trace("Thread will not be affected by runtime costing")
            return null
        }
        return currentThread
    }

    /**
     * Increment the accumulated cost by an integer.
     */
    fun increment(incrementBy: Int) = increment(incrementBy.toLong())

    /**
     * Increment the accumulated cost by a long integer.
     */
    fun increment(incrementBy: Long) = incrementAndCheck(incrementBy)

    /**
     * Increment the accumulated cost by one.
     */
    fun increment() = increment(1L)

    private companion object {
        /**
         * A set of threads to which cost accounting will be disabled.
         */
        private val filteredThreads: List<Thread> = emptyList()

        private val logger = loggerFor<RuntimeCost>()
    }
}
