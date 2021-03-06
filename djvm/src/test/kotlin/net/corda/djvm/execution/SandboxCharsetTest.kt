package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `test loading charsets`(charsetName: String) = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(LookupCharset::class.java)
            .apply(charsetName)
        assertThat(result).isEqualTo(charsetName)
    }

    @Test
    fun `test unknown encoding`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val exception = assertThrows<RuntimeException> {
            taskFactory.create(LookupCharset::class.java)
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
    fun `test string encoding`(charsetName: String) = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(EncodeString::class.java).apply(charsetName)
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
    fun `test string decoding`(charsetName: String) = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(DecodeString::class.java).apply(charsetName)
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
    fun `test default charset`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(DefaultCharset::class.java).apply(null)
        assertThat(result).isEqualTo("UTF-8")
    }

    class DefaultCharset : Function<Void?, String> {
        override fun apply(input: Void?): String {
            return Charset.defaultCharset().name()
        }
    }
}
