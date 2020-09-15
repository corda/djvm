package net.corda.djvm.references

import net.corda.djvm.CordaInternal
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterModule
import java.util.function.Consumer

/**
 * Alias for a handler which will replace an entire
 * method body with a block of byte-code.
 */
typealias MethodBody = Consumer<EmitterModule>

/**
 * Representation of a class member.
 *
 * @property access The access flags of the member.
 * @property className The name of the owning class.
 * @property memberName The name of the member.
 * @property descriptor The descriptor of the member.
 * @property genericsDetails Details about generics used; the "signature".
 * @property annotations The names of the annotations the member is attributed.
 * @property exceptions The names of the exceptions that the member can throw.
 * @property value The default value of a field.
 * @property body One or more handlers to replace the method body with new byte-code.
 * @property runtimeContext Local runtime "state" objects for each [Emitter].
 */
@CordaInternal
class Member(
    override val access: Int,
    override val className: String,
    override val memberName: String,
    override val descriptor: String,
    override val genericsDetails: String,
    val annotations: MutableSet<String> = mutableSetOf(),
    override val exceptions: Set<String> = emptySet(),
    override val value: Any? = null,
    override val body: List<MethodBody> = emptyList(),
    val runtimeContext: MutableMap<Emitter, Any> = mutableMapOf()
) : ImmutableMember {
    override fun toMutable(): Copier = Copier()

    @CordaInternal
    inner class Copier {
        fun copy(
            access: Int = this@Member.access,
            className: String = this@Member.className,
            memberName: String = this@Member.memberName,
            descriptor: String = this@Member.descriptor,
            genericsDetails: String = this@Member.genericsDetails,
            exceptions: Set<String> = this@Member.exceptions,
            value: Any? = this@Member.value,
            body: List<MethodBody> = this@Member.body
        ): ImmutableMember = Member(
            access = access,
            className = className,
            memberName = memberName,
            descriptor = descriptor,
            genericsDetails = genericsDetails,
            annotations = this@Member.annotations,
            exceptions = exceptions,
            value = value,
            body = body,
            runtimeContext = this@Member.runtimeContext
        )
    }
}

@CordaInternal
interface ImmutableMember : MemberInformation, EntityWithAccessFlag {
    override val access: Int
    override val className: String
    override val memberName: String
    override val descriptor: String
    val genericsDetails: String
    val exceptions: Set<String>
    val value: Any?
    val body: List<MethodBody>

    fun toMutable(): Member.Copier
}