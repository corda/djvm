package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.EntityWithAccessFlag
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.rules.MemberRule
import net.corda.djvm.validation.RuleContext
import org.objectweb.asm.Opcodes.ACC_STRICT
import java.lang.reflect.Modifier

/**
 * Definition provider that ensures that all methods use strict floating-point arithmetic in the sandbox.
 *
 * Note: Future JVM releases may make this pass obsolete; https://bugs.openjdk.java.net/browse/JDK-8175916.
 */
object AlwaysUseStrictFloatingPointArithmetic : MemberRule(), MemberDefinitionProvider {

    override fun validate(context: RuleContext, member: ImmutableMember) = context.validate {
        if (member.isMethod && isConcrete(context.clazz)) {
            trace("Strict floating-point arithmetic will be applied") given ((member.access and ACC_STRICT) == 0)
        }
    }

    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember) = when {
        member.isMethod && isConcrete(context.clazz) -> member.toMutable().copy(access = member.access or ACC_STRICT)
        else -> member
    }

    private fun isConcrete(entity: EntityWithAccessFlag) = !Modifier.isAbstract(entity.access)

}
