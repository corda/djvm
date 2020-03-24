package com.example.testing

import net.corda.core.crypto.Crypto
import java.security.PublicKey
import java.util.function.Function

class VerifySignature : Function<Array<Any?>, Boolean> {
    override fun apply(inputs: Array<Any?>): Boolean {
        val schemeCodeName = inputs[0] as String
        val publicKey = inputs[1] as PublicKey
        val signature = inputs[2] as ByteArray
        val clearData = inputs[3] as ByteArray
        return Crypto.doVerify(schemeCodeName, publicKey, signature, clearData)
    }
}
