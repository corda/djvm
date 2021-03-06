package net.corda.djvm.execution

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.messages.MessageCollection
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.rewiring.SandboxClassLoadingException
import net.corda.djvm.utilities.loggerFor
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.Function
import kotlin.concurrent.thread

/**
 * Container for running a task in an isolated environment.
 */
class IsolatedTask(
    private val identifier: String,
    private val context: SandboxRuntimeContext
) {
    constructor(identifier: String, configuration: SandboxConfiguration)
        : this(identifier, SandboxRuntimeContext(configuration))

    /**
     * Run an action in an isolated environment.
     */
    fun <T> run(action: Function<SandboxClassLoader, T?>): Result<T> {
        val threadName = "DJVM-$identifier-${uniqueIdentifier.getAndIncrement()}"
        var output: T? = null
        var costs = CostSummary.empty
        var exception: Throwable? = null
        thread(start = false, isDaemon = true, name = threadName) {
            logger.trace("Entering isolated runtime environment...")
            context.use(Consumer { ctx ->
                output = try {
                    action.apply(ctx.classLoader)
                } finally {
                    costs = CostSummary(ctx.runtimeCosts)
                }
            })
            logger.trace("Exiting isolated runtime environment...")
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex ->
                logger.error("Exception caught in isolated runtime environment", ex)
                exception = (ex as? LinkageError)?.cause ?: ex
            }
            start()
            join()
        }
        val messages = exception.let {
            when (it) {
                is SandboxClassLoadingException -> it.messages
                is SandboxException -> {
                    when (it.exception) {
                        is SandboxClassLoadingException -> it.exception.messages
                        else -> null
                    }
                }
                else -> null
            }
        } ?: MessageCollection()
        return Result(threadName, output, costs, messages, exception)
    }

    /**
     * The result of a run of an [IsolatedTask].
     *
     * @property identifier The identifier of the [IsolatedTask].
     * @property output The result of the run, if successful.
     * @property costs Captured runtime costs as reported at the end of the run.
     * @property messages The messages collated during the run.
     * @property exception This holds any exceptions that might get thrown during execution.
     */
    data class Result<T>(
            val identifier: String,
            val output: T?,
            val costs: CostSummary,
            val messages: MessageCollection,
            val exception: Throwable?
    )

    // TODO Caching can transcend thread-local contexts by taking the sandbox configuration into account in the key derivation

    private companion object {

        /**
         * An atomically incrementing identifier used to uniquely identify each runnable.
         */
        private val uniqueIdentifier = AtomicLong(0)

        private val logger = loggerFor<IsolatedTask>()

    }

}