package com.example.testing

import net.corda.core.internal.hash
import java.security.PublicKey
import java.util.function.Function

class PublicKeyFunction : Function<PublicKey, String> {
    override fun apply(key: PublicKey): String {
        return "PublicKey: Format='${key.format}', Algorithm='${key.algorithm}', Hash='${key.hash}'"
    }
}
