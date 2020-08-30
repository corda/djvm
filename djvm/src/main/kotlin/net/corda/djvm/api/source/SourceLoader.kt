package net.corda.djvm.api.source

import java.net.URL

abstract class SourceLoader(parent: ClassLoader?) : ClassLoader(parent) {
    abstract val codeLocations: Set<CodeLocation>

    abstract fun getAllURLs(): Set<URL>

    /**
     * Load the underlying [ClassHeader] for the sandbox class
     * with the specified binary name.
     */
    @Throws(ClassNotFoundException::class)
    abstract fun loadSourceHeader(name: String): ClassHeader

    /**
     * Load the [ClassHeader] for the source class
     * with the specified binary name.
     */
    @Throws(ClassNotFoundException::class)
    abstract fun loadClassHeader(name: String): ClassHeader
}

