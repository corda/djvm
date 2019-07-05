package net.corda.djvm.validation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.references.*

/**
 * The context in which a rule is validated.
 *
 * @property analysisContext The context in which a class and its members are analyzed.
 */
@Suppress("unused")
open class RuleContext(
        private val analysisContext: AnalysisRuntimeContext
) : ConstraintProvider(analysisContext) {
    fun formatFor(member: MemberInformation): String = analysisContext.configuration.formatFor(member)

    /**
     * The class currently being analysed.
     */
    val clazz: ClassRepresentation
        get() = analysisContext.clazz

    /**
     * The member currently being analysed, if any.
     */
    val member: Member?
        get() = analysisContext.member

    /**
     * The current source location.
     */
    val location: SourceLocation
        get() = analysisContext.location

    /**
     * The configured whitelist.
     */
    val whitelist: Whitelist
        get() = analysisContext.configuration.whitelist

    /**
     * Utilities for dealing with classes.
     */
    val classModule: ClassModule
        get() = analysisContext.configuration.classModule

    /**
     * Utilities for dealing with members.
     */
    val memberModule: MemberModule
        get() = analysisContext.configuration.memberModule

    /**
     * Check whether the class has been explicitly defined in the sandbox namespace.
     */
    fun isSandboxClass(className: String): Boolean = analysisContext.configuration.isSandboxClass(className)

    /**
     * Set up and execute a rule validation block.
     */
    fun validate(action: RuleContext.() -> Unit) {
        action(this)
    }

}
