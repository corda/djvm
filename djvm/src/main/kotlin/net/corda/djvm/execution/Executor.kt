package net.corda.djvm.execution

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.source.ClassSource

abstract class Executor<in INPUT, out OUTPUT>(protected val configuration: SandboxConfiguration) {

    @Throws(Exception::class)
    abstract fun run(runnableClass: ClassSource, input: INPUT): ExecutionSummaryWithResult<OUTPUT>

}