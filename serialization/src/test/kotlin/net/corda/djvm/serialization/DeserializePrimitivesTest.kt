package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(LocalSerialization::class)
class DeserializePrimitivesTest : TestBase(KOTLIN) {
    @Test
    fun `test naked uuid`() {
        val uuid = UUID.randomUUID()
        val data = uuid.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxUUID = data.deserializeFor(classLoader)
            assertEquals(uuid.toString(), sandboxUUID.toString())
            assertEquals("sandbox.${uuid::class.java.name}", sandboxUUID::class.java.name)
        }
    }

    @Test
    fun `test wrapped uuid`() {
        val uuid = WrappedUUID(UUID.randomUUID())
        val data = uuid.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxUUID = data.deserializeFor(classLoader)
            assertEquals(uuid.toString(), sandboxUUID.toString())
            assertEquals("sandbox.${uuid::class.java.name}", sandboxUUID::class.java.name)
        }
    }
}

@CordaSerializable
data class WrappedUUID(val uuid: UUID)
