package net.corda.djvm.execution

class ExpectedException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(message: String?) : this(message, null)
    constructor() : this(null, null)
}
