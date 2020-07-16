package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.ImmutableMember
import org.objectweb.asm.Opcodes.*

/**
 * Replace internal APIs with stubs that throw exceptions. Only for non-whitelisted classes.
 */
object StubOutIntrospectiveMethods : MemberDefinitionProvider {
    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember = when {
        member.isMethod && isConcreteApi(member) && isIntrospective(member) && !isAllowedFor(context, member)
             -> member.toMutable().copy(body = member.body + MemberRuleEnforcer(member)::forbidAPI)
        else -> member
    }

    // The method must be public and with a Java implementation.
    private fun isConcreteApi(member: ImmutableMember): Boolean = member.access and (ACC_PUBLIC or ACC_ABSTRACT or ACC_NATIVE) == ACC_PUBLIC

    private fun isIntrospective(member: ImmutableMember): Boolean {
        return member.className.startsWith("java/lang/invoke/")
               || member.className.startsWith("sun/reflect/")
               || member.className == "sun/misc/Unsafe"
    }

    private fun isAllowedFor(context: AnalysisRuntimeContext, member: ImmutableMember): Boolean {
        return context.configuration.getSourceHeader(member.className).isThrowable
    }
}
