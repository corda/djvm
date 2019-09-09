package net.corda.djvm.serialization

import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.util.*
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeOptionalTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing optional with object`() {
        val optional = Optional.of("Hello World!")
        val data = optional.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxOptional = data.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            val showOptional = classLoader.createTaskFor(taskFactory, ShowOptional::class.java)
            val result = showOptional.apply(sandboxOptional) ?: fail("Result cannot be null")

            assertEquals("Optional -> Optional[Hello World!]", result.toString())
            assertEquals(SANDBOX_STRING, result::class.java.name)
        }
    }

    @Test
    fun `test deserializing optional without object`() {
        val optional = Optional.empty<String>()
        val data = optional.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxOptional = data.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            val showOptional = classLoader.createTaskFor(taskFactory, ShowOptional::class.java)
            val result = showOptional.apply(sandboxOptional) ?: fail("Result cannot be null")

            assertEquals("Optional -> Optional.empty", result.toString())
            assertEquals(SANDBOX_STRING, result::class.java.name)
        }
    }
}

class ShowOptional : Function<Optional<*>, String> {
    override fun apply(optional: Optional<*>): String {
        return "Optional -> $optional"
    }
}
