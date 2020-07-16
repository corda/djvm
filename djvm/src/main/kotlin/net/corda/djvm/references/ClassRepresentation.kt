package net.corda.djvm.references

import net.corda.djvm.code.OBJECT_NAME
import java.lang.reflect.Modifier

/**
 * Representation of a class.
 *
 * @property apiVersion The target API version for which the class was compiled.
 * @property access The access flags of the class.
 * @property name The name of the class.
 * @property superClass The name of the super-class, if any.
 * @property interfaces The names of the interfaces implemented by the class.
 * @property sourceFile The name of the compiled source file, if available.
 * @property genericsDetails Details about generics used.
 * @property members The set of fields and methods implemented in the class.
 * @property annotations The set of annotations applied to the class.
 */
data class ClassRepresentation(
        override val apiVersion: Int,
        override val access: Int,
        override val name: String,
        override val superClass: String = "",
        override val interfaces: List<String> = emptyList(),
        override var sourceFile: String = "",
        override val genericsDetails: String = "",
        val members: MutableMap<String, Member> = mutableMapOf(),
        val annotations: MutableSet<String> = mutableSetOf()
) : EntityWithAccessFlag, ImmutableClass {
    override val isInterface: Boolean
        get() = Modifier.isInterface(access)

    override val hasObjectAsSuperclass: Boolean
        get() = superClass.isEmpty() || superClass == OBJECT_NAME

    override fun toMutable(): Copier = Copier()

    inner class Copier {
        fun copy(
            apiVersion: Int = this@ClassRepresentation.apiVersion,
            access: Int = this@ClassRepresentation.access,
            name: String = this@ClassRepresentation.name,
            superClass: String = this@ClassRepresentation.superClass,
            interfaces: List<String> = this@ClassRepresentation.interfaces,
            sourceFile: String = this@ClassRepresentation.sourceFile,
            genericsDetails: String = this@ClassRepresentation.genericsDetails
        ): ImmutableClass = ClassRepresentation(
            apiVersion = apiVersion,
            access = access,
            name = name,
            superClass = superClass,
            interfaces = interfaces,
            sourceFile = sourceFile,
            genericsDetails = genericsDetails,
            members = this@ClassRepresentation.members,
            annotations = this@ClassRepresentation.annotations
        )
    }
}

interface ImmutableClass : ClassInformation {
    val apiVersion: Int
    val access: Int
    val name: String
    val superClass: String
    val interfaces: List<String>
    var sourceFile: String
    val genericsDetails: String

    fun toMutable(): ClassRepresentation.Copier
}