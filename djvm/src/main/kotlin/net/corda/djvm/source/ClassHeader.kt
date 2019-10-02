package net.corda.djvm.source

import net.corda.djvm.code.THROWABLE_NAME
import org.objectweb.asm.Opcodes.*

/**
 * A "cut to the bone" replacement for [Class] so that we can compute
 * two classes' common superclass without actually loading them.
 */
class ClassHeader(
    val classLoader: SourceClassLoader,
    val name: String,
    val internalName: String,
    val superclass: ClassHeader?,
    val interfaces: Set<ClassHeader>,
    val flags: Int
) {
    private fun isAssignableFromClass(header: ClassHeader): Boolean {
        var current = header
        while (true) {
            if (current.internalName == internalName) {
                return true
            }
            current = current.superclass ?: break
        }
        return false
    }

    private fun isAssignableFromInterface(header: ClassHeader): Boolean {
        return internalName == header.internalName || header.interfaces.any(::isAssignableFromInterface)
    }

    fun isAssignableFrom(header: ClassHeader): Boolean {
        return if (isInterface) {
            isAssignableFromInterface(header)
        } else {
            isAssignableFromClass(header)
        }
    }

    val isInterface: Boolean get() = (flags and ACC_INTERFACE) != 0

    val isThrowable: Boolean get() = internalName == THROWABLE_NAME || (superclass != null && superclass.isThrowable)
}
