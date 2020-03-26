package com.example.testing.tokens

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import java.util.function.Function

class NonFungibleTask : Function<NonFungibleToken, Array<String?>> {
    override fun apply(token: NonFungibleToken): Array<String?> {
        return arrayOf(
            token.toString(),
            token.holder.toString(),
            token.linearId.toString(),
            token.tokenType.toString(),
            token.tokenTypeJarHash?.toString()
        )
    }
}