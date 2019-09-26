package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import java.util.function.Function
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
import java.util.stream.Stream

class BasicCryptoTest : TestBase(KOTLIN) {
    companion object {
        const val SECRET_MESSAGE = "Goodbye, Cruel World!"
    }

    @ValueSource(strings = [ "SHA", "SHA-256", "SHA-384", "SHA-512" ])
    @ParameterizedTest
    fun `test SHA hashing`(algorithmName: String) = sandbox {
        val executor = DeterministicSandboxExecutor<Array<String>, ByteArray>(configuration)
        val summary = executor.run<Hashing>(arrayOf(algorithmName, SECRET_MESSAGE))
        assertThat(summary.result)
            .isEqualTo(MessageDigest.getInstance(algorithmName).digest(SECRET_MESSAGE.toByteArray()))
    }

    class Hashing : Function<Array<String>, ByteArray> {
        override fun apply(input: Array<String>): ByteArray {
            return MessageDigest.getInstance(input[0])
                    .digest(input[1].toByteArray())
        }
    }

    @CsvSource("RSA,SHA256withRSA", "RSA,SHA512withRSA", "DSA,SHA256withDSA")
    @ParameterizedTest
    fun `test signatures`(algorithm: String, algorithmName: String) = sandbox {
        val keyPair = KeyPairGenerator.getInstance(algorithm).genKeyPair()
        val data = SECRET_MESSAGE.toByteArray()
        val signature = Signature.getInstance(algorithmName).run {
            initSign(keyPair.private)
            update(data)
            sign()
        }

        val executor = DeterministicSandboxExecutor<Array<*>, Boolean>(configuration)
        val summary = executor.run<VerifySignature>(
            arrayOf(algorithm, keyPair.public.encoded, algorithmName, data, signature)
        )
        assertThat(summary.result).isTrue()
    }

    class VerifySignature : Function<Array<*>, Boolean> {
        override fun apply(input: Array<*>): Boolean {
            require(input.size == 5) { "Incorrect inputs to VerifySignature: size=${input.size}" }

            val keyFactory = KeyFactory.getInstance(input[0] as String)
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(input[1] as ByteArray))
            return Signature.getInstance(input[2] as String).run {
                initVerify(publicKey)
                update(input[3] as ByteArray)
                verify(input[4] as ByteArray)
            }
        }
    }

    @Test
    fun `test security providers`() = sandbox {
        val executor = DeterministicSandboxExecutor<String, Array<String>>(configuration)
        val summary = executor.run<SecurityProviders>("")
        assertThat(summary.result).isEqualTo(arrayOf("SUN", "SunRsaSign"))
    }

    class SecurityProviders : Function<String, Array<String>> {
        override fun apply(input: String): Array<String> {
            return Security.getProviders().map(Provider::getName).toTypedArray()
        }
    }

    class AlgorithmProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Stream.of(
                Arguments.of("MessageDigest", arrayOf("MD2","MD5","SHA","SHA-224","SHA-256","SHA-384","SHA-512")),
                Arguments.of("Signature", arrayOf(
                    "MD2WITHRSA", "MD5WITHRSA",
                    "NONEWITHDSA",
                    "SHA1WITHDSA", "SHA1WITHRSA",
                    "SHA224WITHDSA", "SHA224WITHRSA",
                    "SHA256WITHDSA", "SHA256WITHRSA",
                    "SHA384WITHRSA",
                    "SHA512WITHRSA"
                )),
                Arguments.of("KeyStore", arrayOf("CASEEXACTJKS", "DKS", "JKS")),
                Arguments.of("Mac", emptyArray<String>()),
                Arguments.of("Cipher", emptyArray<String>())
            )
        }
    }

    @ArgumentsSource(AlgorithmProvider::class)
    @ParameterizedTest
    fun `test service algorithms`(serviceName: String, algorithms: Array<String>) = sandbox {
        val executor = DeterministicSandboxExecutor<String, Array<String>>(configuration)
        val summary = executor.run<ServiceAlgorithms>(serviceName)
        assertThat(summary.result)
            .isEqualTo(algorithms)
    }

    class ServiceAlgorithms : Function<String, Array<String>> {
        override fun apply(serviceName: String): Array<String> {
            return Security.getAlgorithms(serviceName).sorted().toTypedArray()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [ "SUN", "SunRsaSign" ])
    fun `test no secure random for`(serviceName: String) = sandbox {
        val executor = DeterministicSandboxExecutor<String, Double>(configuration)
        val exception = assertThrows<SandboxException> { executor.run<SecureRandomService>(serviceName) }
        assertThat(exception)
            .hasCauseExactlyInstanceOf(Exception::class.java)
            .hasMessage("sandbox.java.security.NoSuchAlgorithmException -> $serviceName SecureRandom not available")
    }

    class SecureRandomService : Function<String, Double> {
        override fun apply(serviceName: String): Double {
            return SecureRandom.getInstance(serviceName).nextDouble()
        }
    }

    @Test
    fun `test secure random instance`() = sandbox {
        val executor = DeterministicSandboxExecutor<ByteArray?, Double>(configuration)
        val exception = assertThrows<SandboxException> { executor.run<SecureRandomInstance>(null) }
        assertThat(exception)
            .hasCauseExactlyInstanceOf(UnsupportedOperationException::class.java)
            .hasMessageContaining("Seed generation disabled")
    }

    class SecureRandomInstance : Function<ByteArray?, Double> {
        override fun apply(seed: ByteArray?): Double {
            return (seed ?.run(::SecureRandom) ?: SecureRandom()).nextDouble()
        }
    }

    @ValueSource(strings = [ "RSA", "DSA" ])
    @ParameterizedTest
    fun `test decode public key`(algorithm: String) = sandbox {
        val executor = DeterministicSandboxExecutor<Array<Any>, ByteArray>(configuration)
        val generator = KeyPairGenerator.getInstance(algorithm)
        val keyPair = generator.genKeyPair()

        val input = keyPair.public.encoded
        assertThat(executor.run<DecodePublicKey>(arrayOf(algorithm, input)).result).isEqualTo(input)
    }

    class DecodePublicKey : Function<Array<Any>, ByteArray> {
        override fun apply(data: Array<Any>): ByteArray {
            val spec = X509EncodedKeySpec(data[1] as ByteArray)
            val keyFactory = KeyFactory.getInstance(data[0] as String)
            val publicKey = keyFactory.generatePublic(spec)
            return publicKey.encoded
        }
    }

    @Test
    fun `test certificates`() {
        val certificate = javaClass.classLoader.getResourceAsStream("testing.cert")?.use { input ->
            input.readBytes()
        } ?: fail("Certificate not found")

        sandbox {
            val executor = DeterministicSandboxExecutor<Array<Any>, String>(configuration)
            val summary = executor.run<DecodeCertificate>(arrayOf("X.509", certificate))
            assertThat(summary.result)
                .isEqualTo("""Certificate:
                             |- type:                X.509
                             |- version:             3
                             |- issuer:              CN=localhost, O=R3, L=London, C=UK
                             |- signature algorithm: SHA256withRSA
                             |  - algorithm OID:     1.2.840.113549.1.1.11
                             |""".trimMargin())
        }
    }

    class DecodeCertificate : Function<Array<Any>, String> {
        override fun apply(data: Array<Any>): String {
            val factory = CertificateFactory.getInstance(data[0] as String)
            val certificate = factory.generateCertificate((data[1] as ByteArray).inputStream()) as X509Certificate
            return """Certificate:
                     |- type:                ${certificate.type}
                     |- version:             ${certificate.version}
                     |- issuer:              ${certificate.issuerX500Principal}
                     |- signature algorithm: ${certificate.sigAlgName}
                     |  - algorithm OID:     ${certificate.sigAlgOID}
                     |""".trimMargin()
        }
    }
}
