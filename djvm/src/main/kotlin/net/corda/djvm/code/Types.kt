@file:JvmName("Types")
package net.corda.djvm.code

import net.corda.djvm.costing.ThresholdViolationError
import net.corda.djvm.rules.RuleViolationError
import org.objectweb.asm.Type

/**
 * These are the priorities for executing [Emitter] instances.
 * Tracing emitters are executed first.
 */
const val EMIT_TRACING: Int = 0
const val EMIT_TRAPPING_EXCEPTIONS: Int = EMIT_TRACING + 1
const val EMIT_HANDLING_EXCEPTIONS: Int = EMIT_TRAPPING_EXCEPTIONS + 1
const val EMIT_DEFAULT: Int = 10

const val EMIT_BEFORE_INVOKE: Int = EMIT_DEFAULT - 2
const val EMIT_AFTER_INVOKE: Int = EMIT_DEFAULT + 2

const val CLASS_NAME = "java/lang/Class"
const val OBJECT_NAME = "java/lang/Object"
const val THROWABLE_NAME = "java/lang/Throwable"
const val ENUM_NAME = "java/lang/Enum"
const val SANDBOX_OBJECT_NAME = "sandbox/java/lang/Object"
const val CLASS_CONSTRUCTOR_NAME = "<clinit>"
const val CONSTRUCTOR_NAME = "<init>"
const val FROM_DJVM = "fromDJVM"

/**
 * The type name of the [sandbox.java.lang.DJVM] class which contains the
 * low-level support functions for running inside the sandbox.
 */
const val DJVM_NAME = "sandbox/java/lang/DJVM"

/**
 * The type name of the [RuntimeCostAccounter] class; referenced from instrumentors.
 */
const val RUNTIME_ACCOUNTER_NAME: String = "sandbox/RuntimeCostAccounter"

/**
 * The internal type name of the [sandbox.java.lang.DJVMException] class.
 * Note that we cannot use [Type.getInternalName] here because that would
 * load our template class into the Application classloader.
 */
const val DJVM_EXCEPTION_NAME: String = "sandbox/java/lang/DJVMException"

const val DJVM_MODIFIED = 1
const val DJVM_ANNOTATION = 2
const val DJVM_SYNTHETIC = 2

val ruleViolationError: String = Type.getInternalName(RuleViolationError::class.java)
val thresholdViolationError: String = Type.getInternalName(ThresholdViolationError::class.java)

/**
 * Local extension method for normalizing a class name.
 */
val String.asPackagePath: String get() = this.replace('/', '.')
val String.asResourcePath: String get() = this.replace('.', '/')

val String.emptyAsNull: String? get() = if (isEmpty()) null else this

inline fun <reified T> Emitter.getMemberContext(context: EmitterContext): T? {
    return context.getMemberContext(this) as? T
}
