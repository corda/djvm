package net.corda.djvm.code.impl

import net.corda.djvm.CordaInternal
import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.references.ClassModule
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.references.MemberModule

/**
 * The context in which an emitter is invoked.
 *
 * @param analysisContext The context in which a class and its members are processed.
 * @param configuration The configuration to used for the analysis.
 * @property emitterModule A module providing code generation functionality that can be used from within an emitter.
 */
@CordaInternal
class EmitterContextImpl(
    private val analysisContext: AnalysisRuntimeContext,
    private val configuration: AnalysisConfiguration,
    val emitterModule: EmitterModuleImpl
) : EmitterContext {

    /**
     * The class currently being analysed.
     */
    override val clazz: ImmutableClass
        get() = analysisContext.clazz

    /**
     * The member currently being analysed, if any.
     */
    override val member: ImmutableMember?
        get() = analysisContext.member

    /**
     * The current source location.
     */
    override val location: SourceLocation
        get() = analysisContext.location

    /**
     * The configured whitelist.
     */
    override val whitelist: Whitelist
        get() = analysisContext.configuration.whitelist

    /**
     * Utilities for dealing with classes.
     */
    override val classModule: ClassModule
        get() = analysisContext.configuration.classModule

    /**
     * Utilities for dealing with members.
     */
    @Suppress("unused")
    override val memberModule: MemberModule
        get() = analysisContext.configuration.memberModule

    /**
     * Return the runtime data associated with both this member and this emitter.
     */
    override fun getMemberContext(emitter: Emitter): Any? {
        return (analysisContext.member ?: return null).runtimeContext.computeIfAbsent(emitter, Emitter::createMemberContext)
    }

    /**
     * Resolve the sandboxed name of a class or interface.
     */
    override fun resolve(typeName: String): String {
        return configuration.classResolver.resolve(typeName)
    }

    override fun resolveDescriptor(descriptor: String): String {
        return configuration.classResolver.resolveDescriptor(descriptor)
    }

}
