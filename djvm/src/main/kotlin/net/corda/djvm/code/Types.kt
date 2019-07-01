@file:JvmName("Types")
package net.corda.djvm.code

import net.corda.djvm.costing.ThresholdViolationError
import org.objectweb.asm.Type
import sandbox.java.lang.DJVMException
import sandbox.net.corda.djvm.rules.RuleViolationError

/**
 * These are the priorities for executing [Emitter] instances.
 * Tracing emitters are executed first.
 */
const val EMIT_TRACING: Int = 0
const val EMIT_TRAPPING_EXCEPTIONS: Int = EMIT_TRACING + 1
const val EMIT_HANDLING_EXCEPTIONS: Int = EMIT_TRAPPING_EXCEPTIONS + 1
const val EMIT_DEFAULT: Int = 10

const val OBJECT_NAME = "java/lang/Object"
const val THROWABLE_NAME = "java/lang/Throwable"

val ruleViolationError: String = Type.getInternalName(RuleViolationError::class.java)
val thresholdViolationError: String = Type.getInternalName(ThresholdViolationError::class.java)
val djvmException: String = Type.getInternalName(DJVMException::class.java)

/**
 * Local extension method for normalizing a class name.
 */
val String.asPackagePath: String get() = this.replace('/', '.')
val String.asResourcePath: String get() = this.replace('.', '/')

val String.emptyAsNull: String? get() = if (isEmpty()) null else this

inline fun <reified T> Emitter.getMemberContext(context: EmitterContext): T? {
    return context.getMemberContext(this) as T?
}
