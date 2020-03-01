package com.example.testing

import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.serialization.djvm.createSandboxSerializationEnv
import net.corda.serialization.djvm.deserializeFor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

@ExtendWith(LocalSerialization::class)
class CryptoTest : TestBase() {
    companion object {
        const val IMPORTANT_MESSAGE = "Very Important Message! Trust Me!"
    }

    class SignatureSchemeProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return Crypto.supportedSignatureSchemes().stream()
                .filter { it != Crypto.COMPOSITE_KEY }
                .map { Arguments.of(it) }
        }
    }

    @ArgumentsSource(SignatureSchemeProvider::class)
    @ParameterizedTest(name = "verifying signature: {index} => {0}")
    fun `test verifying signature`(signatureScheme: SignatureScheme) {
        val clearData = IMPORTANT_MESSAGE.toByteArray()

        val keyPair = Crypto.generateKeyPair(signatureScheme)
        val signature = Crypto.doSign(signatureScheme, keyPair.`private`, clearData)

        val nameData = signatureScheme.schemeCodeName.serialize()
        val keyData = keyPair.`public`.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))
            val sandboxSchemeName = nameData.deserializeFor(classLoader)
            val sandboxKey = keyData.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            val verifier = taskFactory.compose(classLoader.createSandboxFunction()).apply(VerifySignature::class.java)
            val result = verifier.apply(arrayOf(sandboxSchemeName, sandboxKey, signature, clearData))
            assertEquals(true.toString(), result.toString())
        }
    }
}