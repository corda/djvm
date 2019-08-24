package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.charset.Charset
import java.util.function.Function

class SandboxCharsetTest : TestBase(KOTLIN) {
    companion object {
        const val MESSAGE = "Hello World!"
    }

    @ParameterizedTest
    @ValueSource(strings = ["UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII", "windows-1252"])
    fun `test loading charsets`(charsetName: String) = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String>(configuration)
        contractExecutor.run<LookupCharset>(charsetName).apply {
            assertThat(result).isEqualTo(charsetName)
        }
    }

    @Test
    fun `test unknown encoding`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String>(configuration)
        val exception = assertThrows<SandboxException> {
            contractExecutor.run<LookupCharset>("Nonsense-101")
        }
        assertThat(exception)
            .hasCauseExactlyInstanceOf(RuntimeException::class.java)
            .hasMessage("Runtime: sandbox.java.nio.charset.UnsupportedCharsetException -> Nonsense-101")
    }

    class LookupCharset : Function<String, String> {
        override fun apply(charsetName: String): String {
            return Charset.forName(charsetName).displayName()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII", "windows-1252"])
    fun `test string encoding`(charsetName: String) = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, ByteArray>(configuration)
        contractExecutor.run<EncodeString>(charsetName).apply {
            assertNotNull(result)
            assertThat(String(result!!, Charset.forName(charsetName))).isEqualTo(MESSAGE)
        }
    }

    class EncodeString : Function<String, ByteArray> {
        override fun apply(charsetName: String): ByteArray {
            return MESSAGE.toByteArray(Charset.forName(charsetName))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII", "windows-1252"])
    fun `test string decoding`(charsetName: String) = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<String, String>(configuration)
        contractExecutor.run<DecodeString>(charsetName).apply {
            assertThat(result).isEqualTo(MESSAGE)
        }
    }

    class DecodeString : Function<String, String> {
        override fun apply(charsetName: String): String {
            val charset = Charset.forName(charsetName)
            val data = MESSAGE.toByteArray(charset)
            return String(data, charset)
        }
    }

    @Test
    fun `test default charset`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<Void?, String>(configuration)
        contractExecutor.run<DefaultCharset>(null).apply {
            assertThat(result).isEqualTo("UTF-8")
        }
    }

    class DefaultCharset : Function<Void?, String> {
        override fun apply(input: Void?): String {
            return Charset.defaultCharset().name()
        }
    }
}
