package com.example.testing

import net.corda.core.identity.Party
import java.util.function.Function

class PartyToSandbox : Function<Array<ByteArray?>, Party> {
    override fun apply(inputs: Array<ByteArray?>): Party {
        return Party(
            name = CordaX500NameToSandbox().apply(inputs),
            owningKey = PublicKeyDecoder().apply(inputs[6]!!)
        )
    }
}
