package net.corda.djvm

import net.corda.djvm.costing.RuntimeCost.Companion.uncosted
import net.corda.djvm.costing.RuntimeCostSummary
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.rewiring.SandboxClassLoader
import java.lang.invoke.MethodHandle
import java.security.AccessController.doPrivileged
import java.security.PrivilegedAction
import java.util.function.Consumer

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

    private val classResetContext = ClassResetContext()

    @CordaInternal
    internal fun addToReset(clazz: Class<*>, resetMethod: MethodHandle) {
        classResetContext.add(clazz, resetMethod)
    }

    internal val currentResetView: ClassResetContext.View
        @CordaInternal
        get() = classResetContext.currentView

    fun getHashCodeFor(nativeHashCode: Int): Int {
        return classResetContext.getHashCodeFor(nativeHashCode)
    }

    fun intern(key: String, value: Any): Any {
        return classResetContext.intern(key, value)
    }

    fun ready() {
        classResetContext.ready()
    }

    /**
     * Run a set of actions within the provided sandbox context.
     */
    fun use(action: Consumer<SandboxRuntimeContext>) {
        instance = this
        try {
            uncosted(Runnable(classResetContext::reset))
            action.accept(this)
        } finally {
            threadLocalContext.remove()
            doPrivileged(PrivilegedAction(classLoader::close))
        }
    }

    companion object {
        private val threadLocalContext = ThreadLocal<SandboxRuntimeContext?>()

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
