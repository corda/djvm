package net.corda.djvm.source

import java.net.URL
import java.security.CodeSource
import java.security.cert.Certificate

/**
 * @property codeSource A [CodeSource] provided by [SourceClassLoader].
 * @property location The interned [String] value of codeSource.getLocation().toString().
 */
class CodeLocation private constructor(
    val codeSource: CodeSource,
    val location: String
) {
    @Suppress("cast_never_succeeds")
    constructor(sourceURL: URL, location: String) : this(CodeSource(sourceURL, null as? Array<Certificate>), location.intern())
    constructor(sourceURL: URL) : this(sourceURL, sourceURL.toString())

    override fun equals(other: Any?): Boolean {
        return when {
            other === this -> true
            other !is CodeLocation -> false
            else -> location == other.location
        }
    }

    override fun hashCode(): Int {
        return location.hashCode()
    }

    override fun toString(): String {
        return location
    }
}
