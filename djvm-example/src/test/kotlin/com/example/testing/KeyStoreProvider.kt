package com.example.testing

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

class KeyStoreProvider(
    private val keystoreName: String,
    private val keystorePassword: String,
    private val keystoreType: String
) : BeforeAllCallback {
    private lateinit var keystore: KeyStore

    private fun loadKeyStoreResource(resourceName: String, password: CharArray, type: String): KeyStore {
        return KeyStore.getInstance(type).apply {
            KeyStoreProvider::class.java.classLoader.getResourceAsStream(resourceName)?.use { input ->
                load(input, password)
            }
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        keystore = loadKeyStoreResource(keystoreName, keystorePassword.toCharArray(), keystoreType)
    }

    fun getKeyPair(alias: String, password: String): KeyPair {
        val privateKey = keystore.getKey(alias, password.toCharArray()) as PrivateKey
        return KeyPair(keystore.getCertificate(alias).publicKey, privateKey)
    }

    fun getCertificate(alias: String): X509Certificate {
        return keystore.getCertificate(alias) as X509Certificate
    }

    @Suppress("unused")
    fun trustAnchorsFor(vararg aliases: String): Set<TrustAnchor>
        = aliases.mapTo(LinkedHashSet()) { alias -> TrustAnchor(getCertificate(alias), null) }
}
