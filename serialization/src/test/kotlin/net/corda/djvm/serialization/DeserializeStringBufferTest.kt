package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.math.BigInteger
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeStringBufferTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing string buffer`() {
        val buffer = StringBuffer("Hello World!")
        val data = SerializedBytes<Any>(buffer.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxBigInteger = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowStringBuffer::class.java).newInstance(),
                sandboxBigInteger
            ) ?: fail("Result cannot be null")

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
