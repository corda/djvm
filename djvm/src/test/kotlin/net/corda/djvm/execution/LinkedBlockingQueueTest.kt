package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue

import java.util.function.Function

class LinkedBlockingQueueTest : TestBase(KOTLIN) {
    @Test
    fun `elementary linked blocking queue test`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(CreateLinkedBlockingQueue::class.java).apply("Message")
        assertThat(result).isEqualTo(1)
    }

    class CreateLinkedBlockingQueue : Function<String, Int> {
        private val queue = LinkedBlockingQueue<String>()

        override fun apply(message: String): Int {
            queue.add(message)
            return queue.size
        }
    }
}
