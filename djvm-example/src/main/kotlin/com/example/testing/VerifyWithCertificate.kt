package com.example.testing

import net.corda.core.crypto.Crypto
import java.security.Signature
import java.security.cert.Certificate
import java.util.function.Function

class VerifyWithCertificate : Function<Array<Any?>, Boolean> {
    init {
        // Initialise Corda's crypto providers.
        Crypto
    }

    override fun apply(inputs: Array<Any?>): Boolean {
        val algorithm = inputs[0] as String
        val certificate = inputs[1] as Certificate
        val signature = inputs[2] as ByteArray
        val clearData = inputs[3] as ByteArray
        return with(Signature.getInstance(algorithm)) {
            initVerify(certificate)
            update(clearData)
            verify(signature)
        }
    }
}
