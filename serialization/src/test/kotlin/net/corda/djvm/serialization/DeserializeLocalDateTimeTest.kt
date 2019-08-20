package net.corda.djvm.serialization

import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.time.LocalDateTime
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeLocalDateTimeTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing local date-time`() {
        val dateTime = LocalDateTime.now()
        val data = SerializedBytes<Any>(dateTime.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxDateTime = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowLocalDateTime::class.java).newInstance(),
                sandboxDateTime
            ) ?: fail("Result cannot be null")

            assertEquals(dateTime.toString(), result.toString())
            assertEquals(SANDBOX_STRING, result::class.java.name)
        }
    }

    class ShowLocalDateTime : Function<LocalDateTime, String> {
        override fun apply(dateTime: LocalDateTime): String {
            return dateTime.toString()
        }
    }
}
