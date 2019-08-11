package net.corda.djvm.execution

/**
 * @param message The detailed message describing the problem.
 * @param cause The exception underlying this one.
 */
open class SandboxRuntimeException(
    message: String?,
    cause: Throwable?
) : RuntimeException(message, cause) {

    constructor(message: String) : this(message, null)

    @Suppress("unused")
    constructor(cause: Throwable) : this(null, cause)
}
