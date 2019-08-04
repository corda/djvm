package com.example.testing

import net.corda.core.identity.CordaX500Name
import java.util.function.Function

class CordaX500NameToSandbox : Function<Array<ByteArray?>, CordaX500Name> {
    override fun apply(inputs: Array<ByteArray?>): CordaX500Name {
        return CordaX500Name(
            commonName = inputs[0]?.run { String(this) },
            organisationUnit = inputs[1]?.run { String(this) },
            organisation = String(inputs[2]!!),
            locality = String(inputs[3]!!),
            state = inputs[4]?.run { String(this) },
            country = String(inputs[5]!!)
        )
    }
}
