package com.example.testing

import java.util.function.Function

final class BadScalaTask extends Function[String, Long] {
    override def apply(input: String): Long = getClass().getField(input).getLong(this)
}
