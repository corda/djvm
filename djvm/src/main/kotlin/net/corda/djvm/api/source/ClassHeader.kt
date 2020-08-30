package net.corda.djvm.api.source

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