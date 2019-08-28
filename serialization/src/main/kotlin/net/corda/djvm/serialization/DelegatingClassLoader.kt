package net.corda.djvm.serialization

import net.corda.djvm.rewiring.SandboxClassLoader

class DelegatingClassLoader(private val delegate: SandboxClassLoader) : ClassLoader(null) {
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return delegate.loadForSandbox(name).type
    }
}
