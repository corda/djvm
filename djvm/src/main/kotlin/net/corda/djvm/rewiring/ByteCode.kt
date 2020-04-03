package net.corda.djvm.rewiring

import net.corda.djvm.code.DJVM_ANNOTATION
import net.corda.djvm.code.DJVM_MODIFIED
import java.security.CodeSource

/**
 * The byte code representation of a class.
 *
 * @property bytes The raw bytes of the class.
 * @property source Where this byte-code was loaded from.
 * @property isModified Indication of whether the class has been modified as part of loading.
 * @property isAnnotation This byte-code defines an annotation class.
 */
class ByteCode(
    val bytes: ByteArray,
    val source: CodeSource?,
    private val flags: Int
) {
    constructor(bytes: ByteArray, source: CodeSource?) : this(bytes, source, 0)

    val isModified: Boolean get() = (flags and DJVM_MODIFIED) != 0
    val isAnnotation: Boolean get() = (flags and DJVM_ANNOTATION) != 0
}
