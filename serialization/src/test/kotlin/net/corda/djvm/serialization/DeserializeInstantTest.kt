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
import java.time.Instant
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeInstantTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing instant`() {
        val instant = InstantData(Instant.now())
        val data = SerializedBytes<Any>(instant.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxInstant = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowInstant::class.java).newInstance(),
                sandboxInstant
            ) ?: fail("Result cannot be null")

            assertEquals(instant.toString(), result.toString())
        }
    }

    class ShowInstant : Function<InstantData, String> {
        override fun apply(instant: InstantData): String {
            return instant.toString()
        }
    }
}

@CordaSerializable
data class InstantData(val time: Instant)
