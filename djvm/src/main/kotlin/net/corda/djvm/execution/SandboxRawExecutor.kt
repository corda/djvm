package net.corda.djvm.execution

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.messages.Message
import net.corda.djvm.source.ClassSource

class SandboxRawExecutor(configuration: SandboxConfiguration) : Executor<Any?, Any?>(configuration) {
    @Throws(Exception::class)
    override fun run(runnableClass: ClassSource, input: Any?): ExecutionSummaryWithResult<Any?> {
        val result = IsolatedTask(runnableClass.qualifiedClassName, configuration).run {
            // Create the user's task object inside the sandbox.
            val runnable = classLoader.loadClassForSandbox(runnableClass).newInstance()

            val taskFactory = classLoader.createRawTaskFactory()
            val task = taskFactory.apply(runnable)

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
