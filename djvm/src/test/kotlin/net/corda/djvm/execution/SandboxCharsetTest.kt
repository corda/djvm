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
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<String, String, LookupCharset>(taskFactory)
            .apply(charsetName)
        assertThat(result).isEqualTo(charsetName)
    }

    @Test
    fun `test unknown encoding`() = parentedSandbox {
        val taskFactory = classLoader.createTaskFactory()
        val exception = assertThrows<RuntimeException> {
            classLoader.typedTaskFor<String, String, LookupCharset>(taskFactory)
                .apply("Nonsense-101")
        }
        assertThat(exception)
            .isExactlyInstanceOf(RuntimeException::class.java)
            .hasMessage("sandbox.java.nio.charset.UnsupportedCharsetException -> Nonsense-101")
    }

    class LookupCharset : Function<String, String> {
        override fun apply(charsetName: String): String {
            return Charset.forName(charsetName).displayName()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII", "windows-1252"])
    fun `test string encoding`(charsetName: String) = parentedSandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<String, ByteArray, EncodeString>(taskFactory).apply(charsetName)
        assertNotNull(result)
        assertThat(String(result, Charset.forName(charsetName))).isEqualTo(MESSAGE)
    }

    class EncodeString : Function<String, ByteArray> {
        override fun apply(charsetName: String): ByteArray {
            return MESSAGE.toByteArray(Charset.forName(charsetName))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII", "windows-1252"])
    fun `test string decoding`(charsetName: String) = parentedSandbox {
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<String, String, DecodeString>(taskFactory).apply(charsetName)
        assertThat(result).isEqualTo(MESSAGE)
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
        val taskFactory = classLoader.createTaskFactory()
        val result = classLoader.typedTaskFor<Void?, String, DefaultCharset>(taskFactory).apply(null)
        assertThat(result).isEqualTo("UTF-8")
    }

    class DefaultCharset : Function<Void?, String> {
        override fun apply(input: Void?): String {
            return Charset.defaultCharset().name()
        }
    }
}
