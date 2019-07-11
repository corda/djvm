package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue

import java.util.function.Function

class LinkedBlockingQueueTest : TestBase(KOTLIN) {
    @Test
    fun `elementary linked blocking queue test`() = parentedSandbox {
        val executor = DeterministicSandboxExecutor<String, Int>(configuration)
        val summary = executor.run<CreateLinkedBlockingQueue>("Message")
        assertThat(summary.result).isEqualTo(1)
    }

    class CreateLinkedBlockingQueue : Function<String, Int> {
        private val queue = LinkedBlockingQueue<String>()

        override fun apply(message: String): Int {
            queue.add(message)
            return queue.size
        }
    }
}
