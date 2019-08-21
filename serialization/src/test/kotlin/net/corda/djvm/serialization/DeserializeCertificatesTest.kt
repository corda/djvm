package net.corda.djvm.serialization

import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.security.cert.CertPath
import java.util.function.Function
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.security.cert.X509Certificate

@ExtendWith(LocalSerialization::class)
class DeserializeCertificatesTest : TestBase(KOTLIN) {
    @Test
    fun `test deserialize certificate path`() {
        val certPath = CertificateFactory.getInstance("X.509")
            .generateCertPath(emptyList())
        val data = certPath.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxCertPath = data.deserializeFor(classLoader)

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowCertPath::class.java).newInstance(),
                sandboxCertPath
            ) ?: fail("Result cannot be null")

            assertEquals(ShowCertPath().apply(certPath), result.toString())
            assertThat(result::class.java.name).startsWith("sandbox.")
        }
    }

    class ShowCertPath : Function<CertPath, String> {
        override fun apply(certPath: CertPath): String {
            return "CertPath -> $certPath"
        }
    }

    @Disabled
    @Test
    fun `test deserialize X509 certificate`() {
        val certificate = javaClass.classLoader.getResourceAsStream("cert.pem")?.use { input ->
            CertificateFactory.getInstance("X.509")
                .generateCertificate(input) as X509Certificate
        } ?: fail("Certificate not found")
        val data = certificate.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxCertificate = data.deserializeFor(classLoader)

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowCertificate::class.java).newInstance(),
                sandboxCertificate
            ) ?: fail("Result cannot be null")

            assertEquals(ShowCertificate().apply(certificate), result.toString())
            assertThat(result::class.java.name).startsWith("sandbox.")
        }
    }

    class ShowCertificate : Function<X509Certificate, String> {
        override fun apply(certificate: X509Certificate): String {
            return "X.509 Certificate -> $certificate"
        }
    }

    @Disabled
    @Test
    fun `test X509 CRL`() {
        val crl = javaClass.classLoader.getResourceAsStream("crl.pem")?.use { input ->
            CertificateFactory.getInstance("X.509")
                .generateCRL(input) as X509CRL
        } ?: fail("Certificate not found")
        val data = crl.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxCRL = data.deserializeFor(classLoader)

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowCRL::class.java).newInstance(),
                sandboxCRL
            ) ?: fail("Result cannot be null")

            assertEquals(ShowCRL().apply(crl), result.toString())
            assertThat(result::class.java.name).startsWith("sandbox.")
        }
    }

    class ShowCRL : Function<X509CRL, String> {
        override fun apply(crl: X509CRL): String {
            return "X.509 CRL -> $crl"
        }
    }
}
