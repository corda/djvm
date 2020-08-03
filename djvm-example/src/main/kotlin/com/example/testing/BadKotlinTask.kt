package com.example.testing

import java.util.function.Function

class BadKotlinTask : Function<String, Long> {
    override fun apply(input: String): Long {
        return javaClass.getDeclaredField(input).getLong(this)
    }
}