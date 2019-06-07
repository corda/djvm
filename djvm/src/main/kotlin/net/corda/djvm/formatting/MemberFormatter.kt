package net.corda.djvm.formatting

import net.corda.djvm.references.ClassModule
import net.corda.djvm.references.MemberInformation
import net.corda.djvm.references.MemberModule

/**
 * Functionality for formatting a member.
 */
class MemberFormatter(
        private val classModule: ClassModule = ClassModule(),
        private val memberModule: MemberModule = MemberModule()
) {

    /**
     * Format a member.
     */
    fun format(member: MemberInformation): String {
        val className = classModule.getFormattedClassName(member.className)
        val memberName = if (memberModule.isConstructor(member)) {
            ""
        } else {
            ".${member.memberName}"
        }
        return if (memberModule.isField(member)) {
            "$className$memberName"
        } else {
            "$className$memberName(${format(member.descriptor)})"
        }
    }

    /**
     * Format a member's descriptor.
     */
    fun format(abbreviatedDescriptor: String): String {
        var level = 0
        val stringBuilder = StringBuilder()
        for (char in abbreviatedDescriptor) {
            if (char == ')') {
                level -= 1
            }
            if (level >= 1) {
                stringBuilder.append(char)
            }
            if (char == '(') {
                level += 1
            }
        }
        return generateMemberDescriptor(stringBuilder.toString())
    }

    /**
     * Check whether or not a descriptor is for a method.
     */
    fun isMethod(abbreviatedDescriptor: String): Boolean {
        return abbreviatedDescriptor.startsWith('(')
    }

    /**
     * Get the short representation of a class name.
     */
    fun getShortClassName(fullClassName: String): String {
        return classModule.getShortName(fullClassName)
    }

    /**
     * Generate a prettified version of a native descriptor.
     */
    private fun generateMemberDescriptor(abbreviatedDescriptor: String): String {
        return classModule.getTypes(abbreviatedDescriptor).joinToString(", ") {
            classModule.getShortName(it)
        }
    }

}
