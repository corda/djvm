@file:JvmName("Types")
package net.corda.djvm.code

import net.corda.djvm.costing.ThresholdViolationError
import net.corda.djvm.rules.RuleViolationError
import org.objectweb.asm.Type
import sandbox.java.lang.DJVMException

/**
 * These are the priorities for executing [Emitter] instances.
 * Tracing emitters are executed first.
 */
const val EMIT_TRACING: Int = 0
const val EMIT_TRAPPING_EXCEPTIONS: Int = EMIT_TRACING + 1
const val EMIT_HANDLING_EXCEPTIONS: Int = EMIT_TRAPPING_EXCEPTIONS + 1
const val EMIT_DEFAULT: Int = 10

const val EMIT_BEFORE_INVOKE: Int  = EMIT_DEFAULT - 2
const val EMIT_AFTER_INVOKE: Int = EMIT_DEFAULT + 2

const val OBJECT_NAME = "java/lang/Object"
const val THROWABLE_NAME = "java/lang/Throwable"
const val SANDBOX_OBJECT_NAME = "sandbox/java/lang/Object";
const val CLASS_CONSTRUCTOR_NAME = "<clinit>"
const val CONSTRUCTOR_NAME = "<init>"

/**
 * The type name of the [sandbox.java.lang.DJVM] class which contains the
 * low-level support functions for running inside the sandbox.
 */
const val DJVM_NAME = "sandbox/java/lang/DJVM"

/**
 * The type name of the [RuntimeCostAccounter] class; referenced from instrumentors.
 */
const val RUNTIME_ACCOUNTER_NAME: String = "sandbox/RuntimeCostAccounter"

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
    return context.getMemberContext(this) as? T
}
