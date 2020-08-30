package net.corda.djvm.source

/**
 * A "cut to the bone" replacement for [Class] so that we can compute
 * two classes' common superclass without actually loading them.
 * It also allows us to recognise [Enum] and [Annotation] types.
 */
interface ClassHeader {
    val name: String
    val internalName: String

    val superclass: ClassHeader?
    val interfaces: Set<ClassHeader>

    val isInterface: Boolean
    val isThrowable: Boolean
    val isAnnotation: Boolean
    val isEnum: Boolean

    fun isAssignableFrom(header: ClassHeader): Boolean
}
