package net.corda.djvm.execution

import java.io.InputStream
import java.io.NotSerializableException
import java.util.function.Function
import java.util.function.Supplier
import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

class ImportSupplierTaskTest : TestBase(KOTLIN) {
    companion object {
        private const val MESSAGE = "Hello from the outside World!"
    }

    /**
     * Test we can import a [java.io.InputStream] to be
     * consumed inside the sandbox as [sandbox.java.io.InputStream].
     */
    @Test
    fun `import stream from outside sandbox`() = sandbox {
        val taskFactory = classLoader.createRawTaskFactory()
        val readStream = classLoader.createSandboxFunction().apply(ReadInputStream::class.java)
        val streamSupplier = classLoader.createForImport(
            Supplier { classLoader.createBasicInput().apply(MESSAGE.byteInputStream()) }
        )
        val sandboxTask = taskFactory.apply(readStream)
        val result = sandboxTask.apply(streamSupplier) ?: fail("Result is missing!")
        assertEquals("sandbox.java.lang.String", result::class.java.name)
        assertEquals(MESSAGE, result.toString())

        // And check that we're handling nulls correctly too.
        val nullSupplier = classLoader.createForImport(Supplier { null })
        assertNull(sandboxTask.apply(nullSupplier), "Task does not handle a supplier of null.")
        assertNull(sandboxTask.apply(null), "Task does not handle null correctly.")
    }

    class ReadInputStream : Function<Supplier<InputStream?>?, String?> {
        override fun apply(input: Supplier<InputStream?>?): String? {
            return input?.get()?.use {
                String(it.readBytes())
            }
        }
    }

    @Test
    fun `failing import from outside sandbox`() = sandbox {
        val streamSupplier = classLoader.createForImport(
            Supplier { throw NotSerializableException("Corrupt stream!") }
        )
        val readStream = classLoader.createSandboxFunction().apply(ReadInputStream::class.java)
        val sandboxTask = classLoader.createRawTaskFactory().apply(readStream)
        val ex = assertThrows<RuntimeException> { sandboxTask.apply(streamSupplier) }
        assertThat(ex)
            .isExactlyInstanceOf(RuntimeException::class.java)
            .hasMessage("java.io.NotSerializableException -> Corrupt stream!")
            .hasStackTraceContaining("sandbox.ImportSupplierTask")
    }
}
