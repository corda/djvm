package net.corda.djvm.analysis

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.SANDBOX_PREFIX
import net.corda.djvm.formatting.MemberFormatter
import net.corda.djvm.references.ClassModule
import net.corda.djvm.references.MemberInformation
import net.corda.djvm.references.MemberModule

/**
 * Representation of the source location of a class, member or instruction.
 *
 * @property className The name of the class.
 * @property sourceFile The file containing the source of the compiled class.
 * @property memberName The name of the field or method.
 * @property descriptor The descriptor of the field or method.
 * @property lineNumber The index of the line from which the instruction was compiled.
 */
data class SourceLocation(
        override val className: String,
        val sourceFile: String,
        override val memberName: String,
        override val descriptor: String,
        val lineNumber: Int
) : MemberInformation {

    /**
     * Check whether or not information about the source file is available.
     */
    val hasSourceFile: Boolean
        get() = sourceFile.isNotBlank()

    /**
     * Check whether or not information about the line number is available.
     */
    val hasLineNumber: Boolean
        get() = lineNumber != 0

    /**
     * Get a string representation of the source location.
     */
    override fun toString(): String {
        return StringBuilder().apply {
            append(className.removePrefix(SANDBOX_PREFIX))
            if (memberName.isNotBlank()) {
                append(".$memberName")
                if (memberFormatter.isMethod(descriptor)) {
                    append("(${memberFormatter.format(descriptor)})")
                }
            }
        }.toString()
    }

    /**
     * Get a formatted string representation of the source location.
     */
    fun format(): String {
        if (className.isBlank()) {
            return ""
        }
        return StringBuilder().apply {
            append("@|blue ")
            append(if (hasSourceFile) {
                sourceFile
            } else {
                className
            }.removePrefix(SANDBOX_PREFIX))
            append("|@")
            if (hasLineNumber) {
                append(" on @|yellow line $lineNumber|@")
            }
            if (memberName.isNotBlank()) {
                append(" in @|green ")
                if (hasSourceFile) {
                    append("${memberFormatter.getShortClassName(className)}.$memberName")
                } else {
                    append(memberName)
                }
                if (memberFormatter.isMethod(descriptor)) {
                    append("(${memberFormatter.format(descriptor)})")
                }
                append("|@")
            }
        }.toString()
    }

    @Suppress("unused")
    class Builder(private val className: String) {
        constructor() : this("")

        private var sourceFile: String = ""
        private var memberName: String = ""
        private var descriptor: String = ""
        private var lineNumber: Int = 0

        fun withSourceFile(sourceFile: String): Builder {
            this.sourceFile = sourceFile
            return this
        }

        fun withMemberName(memberName: String): Builder {
            this.memberName = memberName
            return this
        }

        fun withDescriptor(descriptor: String): Builder {
            this.descriptor = descriptor
            return this
        }

        fun withLineNumber(lineNumber: Int): Builder {
            this.lineNumber = lineNumber
            return this
        }

        fun build(): SourceLocation {
            return SourceLocation(
                className = className,
                sourceFile = sourceFile,
                memberName = memberName,
                descriptor = descriptor,
                lineNumber = lineNumber
            )
        }
    }

    private companion object {

        private val memberFormatter = MemberFormatter(ClassModule(), MemberModule())

    }

}