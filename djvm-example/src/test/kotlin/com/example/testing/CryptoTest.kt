package com.example.testing

import com.example.testing.SandboxType.KOTLIN
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.crypto.internal.providerMap
import net.corda.djvm.execution.DeterministicSandboxExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.security.*
import java.util.stream.Stream

class CryptoTest : TestBase(KOTLIN) {
    class SignatureSchemeProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Crypto.supportedSignatureSchemes().stream()
                .filter { it != Crypto.COMPOSITE_KEY }
                .map { Arguments.of(it) }
        }
    }

    @ArgumentsSource(SignatureSchemeProvider::class)
    @ParameterizedTest(name = "{index} => {0}")
    fun `test non-composite public keys`(signatureScheme: SignatureScheme) = sandbox {
        val executor = DeterministicSandboxExecutor<Array<Any>, ByteArray>(configuration)
        val keyPair = signatureScheme.generateKeyPair()
        val input = keyPair.public.encoded
        assertThat(executor.run<DecodePublicKey>(arrayOf(signatureScheme.schemeCodeName, input)).result)
            .isEqualTo(input)
    }

    @Test
    fun `test composite public key`() = sandbox {
        val key1 = Crypto.ECDSA_SECP256K1_SHA256.generateKeyPair().public
        val key2 = Crypto.ECDSA_SECP256R1_SHA256.generateKeyPair().public
        val key3 = Crypto.EDDSA_ED25519_SHA512.generateKeyPair().public

        val compositeKey = CompositeKey.Builder()
            .addKey(key1, weight = 1)
            .addKey(key2, weight = 1)
            .addKey(key3, weight = 1)
            .build(2)

        val executor = DeterministicSandboxExecutor<Array<Any>, ByteArray>(configuration)
        val input = compositeKey.encoded
        assertThat(executor.run<DecodePublicKey>(arrayOf(Crypto.COMPOSITE_KEY.schemeCodeName, input)).result)
            .isEqualTo(input)
    }

    private fun SignatureScheme.generateKeyPair(): KeyPair {
        return KeyPairGenerator.getInstance(algorithmName, providerMap[providerName]).let {
            if (algSpec != null) {
                it.initialize(algSpec, SecureRandom())
            } else {
                it.initialize(keySize!!, SecureRandom())
            }
            it.generateKeyPair()
        }
    }
}
