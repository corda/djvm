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
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeStringTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing string`() {
        val string = StringData("Hello World!")
        val data = SerializedBytes<Any>(string.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxString = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowString::class.java).newInstance(),
                sandboxString
            ) ?: fail("Result cannot be null")

            assertEquals(result.toString(), string.message)
        }
    }

    class ShowString : Function<StringData, String> {
        override fun apply(data: StringData): String {
            return data.message
        }
    }
}

@CordaSerializable
data class StringData(val message: String)
