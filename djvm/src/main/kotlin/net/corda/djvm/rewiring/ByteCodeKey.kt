package net.corda.djvm.rewiring

/**
 * Key object for [ByteCode] values inside the external cache.
 *
 * @property className The fully qualified name of the sandbox class.
 * @property source The location of the source class.
 */
data class ByteCodeKey(
    val className: String,
    val source: String
)
