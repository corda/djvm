@file:JvmName("ClassLoading")
package net.corda.djvm.serialization

import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.source.ClassSource

class DelegatingClassLoader(private val delegate: SandboxClassLoader) : ClassLoader(null) {
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return delegate.loadForSandbox(name).type
    }
}

fun SandboxClassLoader.loadClassForSandbox(clazz: Class<*>): Class<Any> {
    @Suppress("unchecked_cast")
    return loadClassForSandbox(ClassSource.fromClassName(clazz.name)) as Class<Any>
}
