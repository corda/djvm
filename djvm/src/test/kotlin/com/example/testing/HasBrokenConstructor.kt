package com.example.testing

/**
 * The DJVM refuses even to try loading classes from the
 * net.corda.djvm package into the sandbox. And so this
 * class lives in a "safe" package "com.example.testing".
 */
@Suppress("unused")
class HasBrokenConstructor(initialValue: Int) {
    // This constructor should trigger an integer overflow.
    constructor() : this(1)

    val number: Int = Int.MAX_VALUE + initialValue
}
