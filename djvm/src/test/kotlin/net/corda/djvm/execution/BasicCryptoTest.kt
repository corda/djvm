package net.corda.djvm.execution

import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import java.util.function.Function
import org.junit.jupiter.api.Test
import java.security.MessageDigest

@Disabled
class BasicCryptoTest : TestBase() {
    companion object {
        const val SECRET_MESSAGE = "Goodbye, Cruel World!"
    }

    @Test
    fun `test SHA-256 hashing`() = parentedSandbox {
        val contractExecutor = DeterministicSandboxExecutor<ByteArray, ByteArray>(configuration)
        val summary = contractExecutor.run<SHA256>(SECRET_MESSAGE.toByteArray())
        val result = summary.result
        assertThat(result).isEqualTo("sandbox")
    }

    class SHA256 : Function<ByteArray, ByteArray> {
        override fun apply(input: ByteArray): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            return input
        }
    }
}