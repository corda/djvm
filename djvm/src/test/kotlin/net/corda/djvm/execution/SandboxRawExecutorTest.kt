package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import net.corda.djvm.source.ClassSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.util.function.Function

class SandboxRawExecutorTest : TestBase(KOTLIN) {
    @Test
    fun `test raw executor`() = parentedSandbox {
        val executor = SandboxRawExecutor(configuration)
        val inputData = "Hello World!".toByteArray()
        val result = executor.run(ClassSource.fromClassName(BackwardsUppercase::class.java.name), inputData).result
        assertThat(String(result as ByteArray)).isEqualTo("!DLROW OLLEH")
    }

    class BackwardsUppercase : Function<ByteArray, ByteArray> {
        override fun apply(input: ByteArray): ByteArray {
            return String(input).toUpperCase().reversed().toByteArray()
        }
    }

    @Test
    fun `test input unmarshalled`() = parentedSandbox {
        val executor = SandboxRawExecutor(configuration)
        val inputData = "Hello World!".toByteArray()
        val ex = assertThrows<SandboxException>{ executor.run(ClassSource.fromClassName(UnmarshalledInput::class.java.name), inputData) }
        assertThat(ex)
            .hasMessageContaining("[B cannot be cast to sandbox.java.lang.String")
            .hasCauseExactlyInstanceOf(ClassCastException::class.java)
    }

    class UnmarshalledInput : Function<String, ByteArray> {
        override fun apply(input: String): ByteArray {
            return input.reversed().toByteArray()
        }
    }

    @Test
    fun `test output is unmarshalled`() = parentedSandbox {
        val executor = SandboxRawExecutor(configuration)
        val inputData = "Hello World!".toByteArray()
        val result = executor.run(ClassSource.fromClassName(UnmarshalledOutput::class.java.name), inputData).result
                ?: fail("Cannot be null")
        assertThat(result::class.java.name).isEqualTo("sandbox.java.lang.String")
    }

    class UnmarshalledOutput : Function<ByteArray, String> {
        override fun apply(input: ByteArray): String {
            return String(input).reversed()
        }
    }
}