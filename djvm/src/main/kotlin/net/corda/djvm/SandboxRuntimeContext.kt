package net.corda.djvm

import net.corda.djvm.costing.RuntimeCostSummary
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.rewiring.SandboxClassLoader
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_STATIC
import java.lang.invoke.MethodHandle
import java.lang.reflect.Field
import java.security.AccessController.doPrivileged
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.function.Consumer
import java.util.function.Function

/**
 * The context in which a sandboxed operation is run.
 *
 * @property configuration The configuration of the sandbox.
 */
class SandboxRuntimeContext(val configuration: SandboxConfiguration) {

    /**
     * The class loader to use inside the sandbox.
     */
    val classLoader: SandboxClassLoader = doPrivileged(PrivilegedAction {
        SandboxClassLoader.createFor(configuration)
    })

    /**
     * A summary of the currently accumulated runtime costs (for, e.g., memory allocations, invocations, etc.).
     */
    val runtimeCosts = RuntimeCostSummary(configuration.executionProfile ?: ExecutionProfile.UNLIMITED)

    private val classResetter = SandboxClassResetter()

    /**
     * Allow tests to reset the [SandboxRuntimeContext]. This method
     * should not otherwise be usable from either Kotlin or Java.
     */
    @CordaInternal
    internal fun accept(visitor: ResetVisitor) {
        visitor.visit(Runnable(::reset))
    }

    @CordaInternal
    internal fun addToReset(resetMethod: MethodHandle) {
        classResetter.add(Resettable(resetMethod))
    }

    @CordaInternal
    internal fun addToReset(clazz: Class<*>, resetMethod: MethodHandle) {
        try {
            doPrivileged(PrivilegedExceptionAction {
                if (classLoader.contains(clazz)) {
                    val finalFields = clazz.declaredFields.filter(::isStaticConstant)
                    for (field in finalFields) {
                        field.isAccessible = true
                    }
                    classResetter.add(resetMethod, finalFields)
                }
            })
        } catch (e: PrivilegedActionException) {
            throw e.cause ?: e
        }
    }

    private fun isStaticConstant(field: Field): Boolean {
        return (field.modifiers and ACC_STATIC_FINAL == ACC_STATIC_FINAL)
            && !field.type.isPrimitive
            && field.type.name != "sandbox.java.lang.String"
    }

    private var nextHashOffset: Function<in Int, out Int> = Function(::decrementHashOffset)
    private val hashCodes = mutableMapOf<Int, Int>()
    private var objectCounter: Int = 0

    // TODO Instead of using a magic offset below, one could take in a per-context seed
    fun getHashCodeFor(nativeHashCode: Int): Int {
        return hashCodes.computeIfAbsent(nativeHashCode, nextHashOffset)
    }

    fun ready() {
        nextHashOffset = Function(::incrementHashOffset)
        objectCounter = 0
    }

    @Suppress("unused_parameter")
    private fun incrementHashOffset(key: Int): Int {
        return ++objectCounter + MAGIC_HASH_OFFSET
    }

    @Suppress("unused_parameter")
    private fun decrementHashOffset(key: Int): Int {
        return --objectCounter + MAGIC_HASH_OFFSET
    }

    private val internStrings = mutableMapOf<String, Any>()

    fun intern(key: String, value: Any): Any {
        return internStrings.computeIfAbsent(key) { value }
    }

    private fun reset() {
        nextHashOffset = Function(::decrementHashOffset)
        objectCounter = 0
        hashCodes.clear()
        internStrings.clear()
        classResetter.reset()
    }

    /**
     * Run a set of actions within the provided sandbox context.
     */
    fun use(action: Consumer<SandboxRuntimeContext>) {
        instance = this
        try {
            reset()
            action.accept(this)
        } finally {
            threadLocalContext.remove()
            doPrivileged(PrivilegedAction(classLoader::close))
        }
    }

    companion object {
        const val ACC_STATIC_FINAL: Int = ACC_STATIC or ACC_FINAL

        private val threadLocalContext = ThreadLocal<SandboxRuntimeContext?>()
        private const val MAGIC_HASH_OFFSET = 0xfed_c0de

        /**
         * When called from within a sandbox, this returns the context for the current sandbox thread.
         */
        @JvmStatic
        var instance: SandboxRuntimeContext
            get() = threadLocalContext.get()
                ?: throw IllegalStateException("SandboxRuntimeContext has not been initialized before use")
            private set(value) {
                threadLocalContext.set(value)
            }
    }
}
