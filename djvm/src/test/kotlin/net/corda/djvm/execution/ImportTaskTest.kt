package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.io.InputStream
import java.io.NotSerializableException
import java.util.function.Function

class ImportTaskTest : TestBase(KOTLIN) {
    companion object {
        const val MESSAGE = "Goodbye, Cruel World!"
    }

    /**
     * Test we can import a [java.io.InputStream] to be
     * consumed inside the sandbox as [sandbox.java.io.InputStream].
     */
    @Test
    fun `import stream from outside sandbox`() = sandbox {
        val taskFactory = classLoader.createRawTaskFactory()
        val readStream = classLoader.createSandboxFunction().apply(ReadInputStream::class.java)
        val createStream = classLoader.createForImport(
            CreateInputStream().andThen(classLoader.createBasicInput())
        )
        val pipelineTask = taskFactory.apply(createStream)
            .andThen(taskFactory.apply(readStream))
        val result = pipelineTask.apply(MESSAGE) ?: fail("Result is missing!")
        assertEquals("sandbox.java.lang.String", result::class.java.name)
        assertEquals(MESSAGE, result.toString())

        // And check that we're handling nulls correctly too.
        assertNull(pipelineTask.apply(null), "Pipeline does not handle null correctly.")
    }

    class CreateInputStream : Function<String?, InputStream?> {
        override fun apply(input: String?): InputStream? {
            return input?.byteInputStream()
        }
    }

    class ReadInputStream : Function<InputStream?, String?> {
        override fun apply(input: InputStream?): String? {
            return input?.use {
                String(it.readBytes())
            }
        }
    }

    @Test
    fun `failing import from outside sandbox`() = sandbox {
        val importTask = classLoader.createForImport(
            ShowFailingInputStream().andThen(classLoader.createBasicInput())
        )
        val sandboxTask = classLoader.createRawTaskFactory().apply(importTask)
        val ex = assertThrows<RuntimeException> { sandboxTask.apply(MESSAGE.byteInputStream()) }
        assertThat(ex)
            .isExactlyInstanceOf(RuntimeException::class.java)
            .hasMessage("java.io.NotSerializableException -> Corrupt stream!")
            .hasStackTraceContaining(ShowFailingInputStream::class.java.name)
            .hasStackTraceContaining("sandbox.ImportTask")
    }

    class ShowFailingInputStream : Function<InputStream, String> {
        override fun apply(input: InputStream): String {
            throw NotSerializableException("Corrupt stream!")
        }
    }
}