package net.corda.djvm.code

import net.corda.djvm.CordaInternal
import net.corda.djvm.analysis.SourceLocation
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.references.ClassModule
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.references.MemberModule

/**
 * The context in which an emitter is invoked.
 */
@CordaInternal
interface EmitterContext {

    /**
     * The class currently being analysed.
     */
    val clazz: ImmutableClass

    /**
     * The member currently being analysed, if any.
     */
    val member: ImmutableMember?

    /**
     * The current source location.
     */
    val location: SourceLocation

    /**
     * The configured whitelist.
     */
    val whitelist: Whitelist

    /**
     * Utilities for dealing with classes.
     */
    val classModule: ClassModule

    /**
     * Utilities for dealing with members.
     */
    @get:Suppress("unused")
    val memberModule: MemberModule

    /**
     * Return the runtime data associated with both this member and this emitter.
     */
    fun getMemberContext(emitter: Emitter): Any?

    /**
     * Resolve the sandboxed name of a class or interface.
     */
    fun resolve(typeName: String): String

    fun resolveDescriptor(descriptor: String): String

}
