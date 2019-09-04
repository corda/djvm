package net.corda.djvm.execution

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.messages.Message
import net.corda.djvm.source.ClassSource
import java.util.function.Function

class SandboxRawExecutor(configuration: SandboxConfiguration) : Executor<Any?, Any?>(configuration) {
    @Throws(Exception::class)
    override fun run(runnableClass: ClassSource, input: Any?): ExecutionSummaryWithResult<Any?> {
        val result = IsolatedTask(runnableClass.qualifiedClassName, configuration).run {
            // Load the "entry-point" task class into the sandbox.
            val taskClass = classLoader.loadClass("sandbox.RawTask")

            // Create the user's task object inside the sandbox.
            val runnable = classLoader.loadClassForSandbox(runnableClass).newInstance()

            // Fetch this sandbox's instance of Class<Function> so we can retrieve RawTask(Function)
            // and then instantiate the RawTask.
            val functionClass = classLoader.loadClass("sandbox.java.util.function.Function")

            @Suppress("unchecked_cast")
            val task = taskClass.getDeclaredConstructor(functionClass).newInstance(runnable) as Function<Any?, Any?>

            // Execute the task...
            task.apply(input)
        }

        when (result.exception) {
            null -> return ExecutionSummaryWithResult(result.output, result.costs)
            else -> throw SandboxException(
                Message.getMessageFromException(result.exception),
                result.identifier,
                runnableClass,
                ExecutionSummary(result.costs),
                result.exception
            )
        }
    }
}
