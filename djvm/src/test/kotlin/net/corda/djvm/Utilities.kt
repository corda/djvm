@file:JvmName("UtilityFunctions")
package net.corda.djvm

import sandbox.net.corda.djvm.costing.ThresholdViolationError
import sandbox.net.corda.djvm.rules.RuleViolationError

/**
 * Allows us to create a [Utilities] object that we can pin inside the sandbox.
 */
object Utilities {
    const val CANNOT_CATCH = "Can't catch this!"

    @JvmStatic
    fun throwRuleViolationError(): Nothing = throw RuleViolationError(CANNOT_CATCH)

    @JvmStatic
    fun throwThresholdViolationError(): Nothing = throw ThresholdViolationError(CANNOT_CATCH)
}
