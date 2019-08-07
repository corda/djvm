package net.corda.djvm.references

import net.corda.djvm.code.CLASS_CONSTRUCTOR_NAME
import net.corda.djvm.code.CONSTRUCTOR_NAME

/**
 * Member-specific functionality.
 */
@Suppress("unused")
class MemberModule : AnnotationModule() {

    /**
     * Add member definition to class.
     */
    fun addToClass(clazz: ClassRepresentation, member: Member): Member {
        clazz.members[getQualifyingIdentifier(member)] = member
        return member
    }

    /**
     * Get member definition for class. Return `null` if the member does not exist.
     */
    fun getFromClass(clazz: ClassRepresentation, memberName: String, descriptor: String): Member? {
        return clazz.members[getQualifyingIdentifier(memberName, descriptor)]
    }

    /**
     * Check if member is a constructor or a static initialization block.
     */
    fun isConstructor(member: MemberInformation): Boolean {
        return member.memberName == CONSTRUCTOR_NAME            // Instance constructor
                || member.memberName == CLASS_CONSTRUCTOR_NAME  // Static initialization block
    }

    /**
     * Check if member is marked to be deterministic.
     */
    fun isDeterministic(member: Member): Boolean {
        return isDeterministic(member.annotations)
    }

    /**
     * Check if member is marked to be non-deterministic.
     */
    fun isNonDeterministic(member: Member): Boolean {
        return isNonDeterministic(member.annotations)
    }

    /**
     * Return the number of arguments that the member expects, based on its descriptor.
     */
    fun numberOfArguments(descriptor: String): Int {
        var count = 0
        var level = 0
        var isLongName = false
        loop@ for (char in descriptor) {
            when {
                char == '(' -> level += 1
                char == ')' -> level -= 1
                char == '[' -> continue@loop
                !isLongName && char == 'L' -> {
                    if (level == 1) {
                        count += 1
                    }
                    isLongName = true
                }
                isLongName && char == ';' -> {
                    isLongName = false
                }
                else -> {
                    if (level == 1 && !isLongName) {
                        count += 1
                    }
                }
            }
        }
        return count
    }

    /**
     * Check whether a function returns `void` or a value/reference type.
     */
    fun returnsValueOrReference(descriptor: String): Boolean {
        return !descriptor.endsWith(")V")
    }

    /**
     * Find all classes referenced in a member's descriptor.
     */
    fun findReferencedClasses(member: MemberInformation): List<String> {
        val classes = mutableListOf<String>()
        var longName = StringBuilder()
        var isLongName = false
        for (char in member.descriptor) {
            if (char == 'L' && !isLongName) {
                longName = StringBuilder()
                isLongName = true
            } else if (char == ';' && isLongName) {
                classes.add(longName.toString())
                isLongName = false
            } else if (isLongName) {
                longName.append(char)
            }
        }
        return classes
    }

    /**
     * Get the qualifying identifier of the class member.
     */
    fun getQualifyingIdentifier(memberName: String, descriptor: String): String {
        return "$memberName:$descriptor"
    }

    /**
     * Get the qualifying identifier of the class member.
     */
    private fun getQualifyingIdentifier(member: MemberInformation): String {
        return getQualifyingIdentifier(member.memberName, member.descriptor)
    }

}
