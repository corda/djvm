@file:JvmName("Types")
package net.corda.djvm.code

import net.corda.djvm.costing.ThresholdViolationError
import net.corda.djvm.rules.RuleViolationError
import org.objectweb.asm.Type
import java.util.Collections.unmodifiableSet

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
const val CLASSLOADER_NAME = "java/lang/ClassLoader"
const val THROWABLE_NAME = "java/lang/Throwable"
const val ENUM_NAME = "java/lang/Enum"
const val SANDBOX_CLASS_NAME = "sandbox/java/lang/DJVMClass"
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

/**
 * Flags describing contents of [net.corda.djvm.rewiring.ByteCode] objects.
 */
const val DJVM_MODIFIED = 0x0001
const val DJVM_SYNTHETIC = 0x0002
const val DJVM_ANNOTATION = 0x0004

/**
 * The monitor methods are BANNED!
 */
private val MONITOR_METHODS = unmodifiableSet(setOf("notify", "notifyAll", "wait"))

fun isObjectMonitor(name: String, descriptor: String): Boolean {
    return (descriptor == "()V" && name in MONITOR_METHODS)
        || (name == "wait" && (descriptor == "(J)V" || descriptor == "(JI)V"))
}

/**
 * These are the names of methods in [sandbox.java.lang.DJVMClass].
 * They correspond to the [java.lang.Class] methods that we intercept.
 */
private val classMethodThunks = unmodifiableSet(setOf(
    // We only need to intercept this when we sandbox
    // java.lang.Enum because it is package private.
    "enumConstantDirectory",

    // These are all public methods.
    "getAnnotation",
    "getAnnotations",
    "getAnnotationsByType",
    "getCanonicalName",
    "getClassLoader",
    "getDeclaredAnnotation",
    "getDeclaredAnnotations",
    "getDeclaredAnnotationsByType",
    "getEnumConstants",
    "getName",
    "getSimpleName",
    "getTypeName",
    "isAnnotationPresent",
    "isEnum",
    "toGenericString",
    "toString"
))

fun isClassMethodThunk(name: String): Boolean = name in classMethodThunks

@JvmField
val ruleViolationError: String = Type.getInternalName(RuleViolationError::class.java)

@JvmField
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
