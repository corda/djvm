package net.corda.djvm.validation

import net.corda.djvm.CordaInternal
import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.references.*

/**
 * The context in which a rule is validated.
 *
 * @param analysisContext The context in which a class and its members are analyzed.
 */
@Suppress("unused")
@CordaInternal
class RuleContext(
    private val analysisContext: AnalysisRuntimeContext
) : ConstraintProvider(analysisContext) {
    private val configuration = analysisContext.configuration

    fun formatFor(member: MemberInformation): String = configuration.formatFor(member)

    /**
     * The class currently being analysed.
     */
    val clazz: ImmutableClass
        get() = analysisContext.clazz

    /**
     * The current source location.
     */
    val location: SourceLocation
        get() = analysisContext.location

    /**
     * The configured whitelist.
     */
    val whitelist: Whitelist
        get() = configuration.whitelist

    /**
     * Utilities for dealing with classes.
     */
    val classModule: ClassModule
        get() = configuration.classModule

    /**
     * Check whether the class has been explicitly defined in the sandbox namespace.
     */
    fun isSandboxClass(className: String): Boolean = configuration.classResolver.isSandboxClass(className)

    /**
     * Set up and execute a rule validation block.
     */
    fun validate(action: RuleContext.() -> Unit) {
        action(this)
    }

}
