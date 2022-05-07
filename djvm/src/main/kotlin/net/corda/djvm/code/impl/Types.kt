@file:JvmName("Types")
package net.corda.djvm.code.impl

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.costing.ThresholdViolationError
import net.corda.djvm.references.MethodBody
import net.corda.djvm.rules.RuleViolationError
import org.objectweb.asm.Type
import java.util.Collections.unmodifiableSet
import java.util.function.Consumer

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
const val SANDBOX_CLASSLOADER_NAME = "sandbox/java/lang/DJVMClassLoader"
const val SANDBOX_OBJECT_NAME = "sandbox/java/lang/Object"
const val REGISTER_RESET_NAME = "forReset"
const val CLASS_RESET_NAME = "djvm\$reset"
const val CLASS_RESET_DESCRIPTOR = "(Ljava/util/function/BiConsumer;)V"
const val CLASS_CONSTRUCTOR_NAME = "<clinit>"
const val CONSTRUCTOR_NAME = "<init>"
const val FROM_DJVM = "fromDJVM"

/**
 * The type name of the [DJVM][sandbox.java.lang.DJVM] class which contains the
 * low-level support functions for running inside the sandbox.
 */
const val DJVM_NAME = "sandbox/java/lang/DJVM"

/**
 * The type name of the [RuntimeCostAccounter][sandbox] class;
 * referenced from instrumentors.
 */
const val RUNTIME_ACCOUNTER_NAME: String = "sandbox/RuntimeCostAccounter"

/**
 * The internal type name of the [DJVMException][sandbox.java.lang.DJVMException]
 * class. Note that we cannot use [Type.getInternalName] here because that would
 * load our template class into the Application classloader.
 */
const val DJVM_EXCEPTION_NAME: String = "sandbox/java/lang/DJVMException"

/**
 * Flags describing contents of [ByteCode][net.corda.djvm.rewiring.ByteCode] objects.
 */
const val DJVM_MODIFIED = 0x0001
const val DJVM_SYNTHETIC = 0x0002
const val DJVM_ANNOTATION = 0x0004

/**
 * The monitor methods are BANNED!
 */
private val MONITOR_METHODS = unmodifiableSet(setOf("notify", "notifyAll", "wait"))

internal fun isObjectMonitor(name: String, descriptor: String): Boolean {
    return (descriptor == "()V" && name in MONITOR_METHODS)
        || (name == "wait" && (descriptor == "(J)V" || descriptor == "(JI)V"))
}

fun isClassStaticThunk(methodName: String): Boolean = Thunks.isClassStatic(methodName)
fun isClassVirtualThunk(methodName: String): Boolean = Thunks.isClassVirtual(methodName)
fun isClassLoaderStaticThunk(methodName: String): Boolean = Thunks.isClassLoaderStatic(methodName)
fun isClassLoaderVirtualThunk(methodName: String): Boolean = Thunks.isClassLoaderVirtual(methodName)

@JvmField
val ruleViolationError: String = Type.getInternalName(RuleViolationError::class.java)

@JvmField
val thresholdViolationError: String = Type.getInternalName(ThresholdViolationError::class.java)

/**
 * Local extension method for normalizing a class name.
 */
val String.asPackagePath: String get() = this.replace('/', '.')
val String.asResourcePath: String get() = this.replace('.', '/')

val String.emptyAsNull: String?
    @Suppress("deprecation") // Needed when compiling on Java 15
    get() = if (isEmpty()) null else this

inline fun <reified T> Emitter.getMemberContext(context: EmitterContext): T? {
    return context.getMemberContext(this) as? T
}

/**
 * Set up and execute an emitter block for a particular member.
 */
inline fun EmitterContext.emit(action: EmitterModuleImpl.() -> Unit) {
    action((this as EmitterContextImpl).emitterModule)
}

/**
 * This function effectively up-casts [Consumer<EmitterModuleImpl>]
 * to [Consumer<EmitterModule>]. This is to prevent the private
 * [EmitterModuleImpl] type from being referenced in the OSGi-exported
 * [net.corda.djvm.references] package,
 */
@Suppress("unchecked_cast", "nothing_to_inline")
inline fun toMethodBody(noinline f: (EmitterModuleImpl) -> Unit): MethodBody {
    return Consumer(f) as MethodBody
}
