@file:JvmName("Tokens")
package com.example.testing.tokens

import com.r3.corda.lib.tokens.contracts.types.TokenType

@Suppress("EqualsOrHashCode")
class Ruble : TokenType("рубль", 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }
}

@JvmField
val RUB = Ruble()
