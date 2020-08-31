package net.corda.djvm.source.impl

import net.corda.djvm.code.impl.ENUM_NAME
import net.corda.djvm.code.impl.THROWABLE_NAME
import net.corda.djvm.source.ClassHeader
import net.corda.djvm.source.SourceClassLoader
import org.objectweb.asm.Opcodes.*

/**
 * A "cut to the bone" replacement for [Class] so that we can compute
 * two classes' common superclass without actually loading them.
 * It also allows us to recognise [Enum] and [Annotation] types.
 */
class ClassHeaderImpl(
    val classLoader: SourceClassLoader,
    override val name: String,
    override val internalName: String,
    override val superclass: ClassHeader?,
    override val interfaces: Set<ClassHeader>,
    val flags: Int
) : ClassHeader {
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

    override fun isAssignableFrom(header: ClassHeader): Boolean {
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

    override val isInterface: Boolean get() = (flags and ACC_INTERFACE) != 0

    override val isThrowable: Boolean get() = internalName == THROWABLE_NAME || (superclass != null && superclass.isThrowable)

    override val isAnnotation: Boolean get() = (flags and ACC_ANNOTATION) != 0

    override val isEnum: Boolean
        get() = (flags and ACC_ENUM) != 0 && (superclass != null) && (superclass.internalName == ENUM_NAME)

    override fun toString(): String = name
}
