package net.corda.djvm.source

import net.corda.djvm.code.ENUM_NAME
import net.corda.djvm.code.THROWABLE_NAME
import org.objectweb.asm.Opcodes.*

/**
 * A "cut to the bone" replacement for [Class] so that we can compute
 * two classes' common superclass without actually loading them.
 * It also allows us to recognise [Enum] and [Annotation] types.
 */
class ClassHeader(
    val classLoader: SourceClassLoader,
    val name: String,
    val internalName: String,
    val superclass: ClassHeader?,
    val interfaces: Set<ClassHeader>,
    val flags: Int
) {
    private fun matchesClass(clazz: ClassHeader): Boolean {
        return clazz.internalName == internalName
    }

    private fun matchesInterface(iface: ClassHeader): Boolean {
        return iface.interfaces.any(::isAssignableFromInterface)
    }

    private fun isAssignableFromInterface(iface: ClassHeader): Boolean {
        return iface.internalName == internalName || matchesInterface(iface)
    }

    private fun isAssignableFromClass(clazz: ClassHeader, matches: ClassHeader.(ClassHeader) -> Boolean): Boolean {
        var current = clazz
        while (true) {
            if (matches(current)) {
                return true
            }
            current = current.superclass ?: break
        }
        return false
    }

    fun isAssignableFrom(header: ClassHeader): Boolean {
        return if (isInterface) {
            if (header.isInterface) {
                isAssignableFromInterface(header)
            } else {
                isAssignableFromClass(header) { i -> matchesInterface(i) }
            }
        } else if (!header.isInterface) {
            isAssignableFromClass(header) { c -> matchesClass(c) }
        } else {
            false
        }
    }

    val isInterface: Boolean get() = (flags and ACC_INTERFACE) != 0

    val isThrowable: Boolean get() = internalName == THROWABLE_NAME || (superclass != null && superclass.isThrowable)

    val isAnnotation: Boolean get() = (flags and ACC_ANNOTATION) != 0

    val isEnum: Boolean
        get() = (flags and ACC_ENUM) != 0 && (superclass != null) && (superclass.internalName == ENUM_NAME)

    override fun toString(): String = name
}
