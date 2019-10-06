package com.example.testing

import java.io.FileNotFoundException

/**
 * The DJVM refuses even to try loading classes from the
 * net.corda.djvm package into the sandbox. And so this
 * class lives in a "safe" package "com.example.testing".
 */
class HasUserExceptionConstructor {
    init {
        throw FileNotFoundException("missing.dat")
    }
}
