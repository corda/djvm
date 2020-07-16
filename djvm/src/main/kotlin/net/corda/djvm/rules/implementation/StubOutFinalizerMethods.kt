package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.ImmutableMember
import java.lang.reflect.Modifier

/**
 * Rule that replaces a finalize() method with a simple stub.
 */
object StubOutFinalizerMethods : MemberDefinitionProvider {

    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember = when {
        /**
         * Discard any other method body and replace with stub that just returns.
         * Other [MemberDefinitionProvider]s are expected to append to this list
         * and not replace its contents!
         */
        isFinalizer(member) -> member.toMutable().copy(body = listOf(::writeMethodBody))
        else -> member
    }

    private fun writeMethodBody(emitter: EmitterModule): Unit = with(emitter) {
        returnVoid()
    }

    /**
     * No need to rewrite [Object.finalize] or [Enum.finalize]; ignore these.
     */
    private fun isFinalizer(member: ImmutableMember): Boolean
        = member.memberName == "finalize" && member.descriptor == "()V"
            && !member.className.startsWith("java/lang/")
            && !Modifier.isAbstract(member.access)
}
