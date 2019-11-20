package com.example.testing

import java.util.function.Function

final class ScalaTask extends Function[Int, String] {
    private val say = (s: Int) => "Sandbox says: '" + s.toHexString.toUpperCase + "'"

    override def apply(input: Int): String = {
        lazy val s = say(input)
        s
    }
}
