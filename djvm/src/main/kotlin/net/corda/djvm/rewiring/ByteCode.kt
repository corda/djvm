package net.corda.djvm.rewiring

import java.security.CodeSource

/**
 * The byte code representation of a class.
 *
 * @property bytes The raw bytes of the class.
 * @property source Where this byte-code was loaded from.
 * @property isModified Indication of whether the class has been modified as part of loading.
 */
class ByteCode(
    val bytes: ByteArray,
    val source: CodeSource?,
    val isModified: Boolean
)
