package com.example.testing

import java.security.MessageDigest
import java.util.function.Function

final class ScalaDigestTask extends Function[Array[Byte], String] {
    override def apply(input: Array[Byte]): String = {
        MessageDigest.getInstance("SHA-256")
            .digest(input)
            .map("%02x" format _)
            .mkString
    }
}
