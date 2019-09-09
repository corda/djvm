package net.corda.djvm.serialization

import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeStringBufferTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing string buffer`() {
        val buffer = StringBuffer("Hello World!")
        val data = buffer.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxBuffer = data.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            val showStringBuffer = classLoader.createTaskFor(taskFactory, ShowStringBuffer::class.java)
            val result = showStringBuffer.apply(sandboxBuffer) ?: fail("Result cannot be null")

            assertEquals(ShowStringBuffer().apply(buffer), result.toString())
            assertEquals(SANDBOX_STRING, result::class.java.name)
        }
    }

    class ShowStringBuffer : Function<StringBuffer, String> {
        override fun apply(buffer: StringBuffer): String {
            return buffer.toString()
        }
    }
}
