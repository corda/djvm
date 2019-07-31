package com.example.testing

import net.corda.core.crypto.Crypto
import java.util.function.Function

class DecodePublicKey : Function<Array<Any>, ByteArray> {
    override fun apply(data: Array<Any>): ByteArray {
        val publicKey = Crypto.decodePublicKey(data[0] as String, data[1] as ByteArray)
        return publicKey.encoded
    }
}
