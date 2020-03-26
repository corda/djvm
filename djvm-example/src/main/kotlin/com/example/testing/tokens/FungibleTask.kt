package com.example.testing.tokens

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import java.util.function.Function

class FungibleTask : Function<FungibleToken, Array<String?>> {
    override fun apply(token: FungibleToken): Array<String?> {
        return arrayOf(
            token.toString(),
            token.tokenType.toString(),
            token.tokenTypeJarHash?.toString()
        )
    }
}
