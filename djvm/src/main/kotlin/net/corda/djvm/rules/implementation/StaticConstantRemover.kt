package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.ImmutableMember

/**
 * Removes static constant objects that are initialised directly in the byte-code.
 * Currently, the only use-case is for re-initialising [String] fields.
 */
object StaticConstantRemover : MemberDefinitionProvider {

    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember = when {
        isConstantField(member) -> member.toMutable().copy(body = listOf(StringFieldInitializer(member)::writeInitializer), value = null)
        else -> member
    }

    private fun isConstantField(member: ImmutableMember): Boolean = member.value != null && member.descriptor == "Ljava/lang/String;"

    class StringFieldInitializer(private val member: ImmutableMember) {
        fun writeInitializer(emitter: EmitterModule): Unit = with(emitter) {
            val value = member.value ?: return
            loadConstant(value)
            invokeStatic(DJVM_NAME, "intern", "(Ljava/lang/String;)Lsandbox/java/lang/String;", false)
            putStatic(member.className, member.memberName, "Lsandbox/java/lang/String;")
        }
    }
}