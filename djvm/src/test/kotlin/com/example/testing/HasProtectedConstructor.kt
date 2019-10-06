package com.example.testing

/**
 * The DJVM refuses even to try loading classes from the
 * net.corda.djvm package into the sandbox. And so this
 * class lives in a "safe" package "com.example.testing".
 */
open class HasProtectedConstructor(val message: String) {
    @Suppress("unused")
    protected constructor() : this("Protected!")

    override fun toString(): String {
        return "HasProtectedConstructor: message=$message"
    }
}
