package net.corda.djvm.rules.implementation

import net.corda.djvm.references.Member
import net.corda.djvm.rules.MemberRule
import net.corda.djvm.validation.RuleContext

object DisallowSandboxMethods : MemberRule() {
    override fun validate(context: RuleContext, member: Member) = context.validate {
        fail("Class is not allowed to implement toDJVMString()") given (
            member.memberName == "toDJVMString" && member.descriptor == "()Ljava/lang/String;"
        )
        fail("Class is not allowed to implement fromDJVM${member.descriptor}") given (
            member.memberName == "fromDJVM" && member.descriptor.startsWith("()")
        )
    }
}