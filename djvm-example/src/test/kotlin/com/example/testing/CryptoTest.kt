package com.example.testing

import com.example.testing.SandboxType.KOTLIN
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.hash
import net.corda.djvm.execution.DeterministicSandboxExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import kotlin.test.fail

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
        val keyPair = Crypto.generateKeyPair(signatureScheme)
        val input = keyPair.public.encoded
        assertThat(executor.run<TransformPublicKey>(arrayOf(signatureScheme.schemeCodeName, input)).result)
            .isEqualTo(input)
    }

    @Test
    fun `test composite public key`() = sandbox {
        val key1 = Crypto.generateKeyPair(Crypto.ECDSA_SECP256K1_SHA256).public
        val key2 = Crypto.generateKeyPair(Crypto.ECDSA_SECP256R1_SHA256).public
        val key3 = Crypto.generateKeyPair(Crypto.EDDSA_ED25519_SHA512).public

        val compositeKey = CompositeKey.Builder()
            .addKey(key1, weight = 1)
            .addKey(key2, weight = 1)
            .addKey(key3, weight = 1)
            .build(2)

        val executor = DeterministicSandboxExecutor<Array<Any>, ByteArray>(configuration)
        val input = compositeKey.encoded
        assertThat(executor.run<TransformPublicKey>(arrayOf(Crypto.COMPOSITE_KEY.schemeCodeName, input)).result)
            .isEqualTo(input)
    }

    @Test
    fun `test marshalling a public key`() = sandbox {
        val executor = Executor(classLoader)

        val key = Crypto.generateKeyPair(Crypto.ECDSA_SECP256K1_SHA256).public
        val sandboxKey = executor.execute(
            task = executor.toSandboxClass(PublicKeyDecoder::class.java).newInstance(),
            input = key.encoded
        )

        val result = executor.execute(
            task = executor.toSandboxClass(PublicKeyFunction::class.java).newInstance(),
            input = sandboxKey
        ).toString()
        assertThat(result).isEqualTo("Format='${key.format}', Algorithm='${key.algorithm}', Hash='${key.hash}'")
    }

    @Test
    fun `test marshalling a Corda party`() = sandbox {
        val owningKey = Crypto.generateKeyPair(Crypto.ECDSA_SECP256K1_SHA256).public
        val party = Party(CordaX500Name("Alice Corp", "Madrid", "ES"), owningKey)

        val inputs = Array<ByteArray?>(7) { null }
        with(party.name) {
            inputs[0] = commonName?.toByteArray()
            inputs[1] = organisationUnit?.toByteArray()
            inputs[2] = organisation.toByteArray()
            inputs[3] = locality.toByteArray()
            inputs[4] = state?.toByteArray()
            inputs[5] = country.toByteArray()
        }
        inputs[6] = party.owningKey.encoded

        val executor = Executor(classLoader)
        val sandboxParty = executor.execute(
            task = executor.toSandboxClass(PartyToSandbox::class.java).newInstance(),
            input = inputs
        ) ?: fail("Party cannot be null")
        assertEquals("sandbox.net.corda.core.identity.Party", sandboxParty::class.java.name)
        assertEquals("O=Alice Corp, L=Madrid, C=ES", sandboxParty.toString())
    }
}
