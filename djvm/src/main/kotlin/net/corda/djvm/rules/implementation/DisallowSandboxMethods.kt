package net.corda.djvm.rules.implementation

import net.corda.djvm.code.FROM_DJVM
import net.corda.djvm.references.Member
import net.corda.djvm.rules.MemberRule
import net.corda.djvm.validation.RuleContext

/**
 * Disallow loading of classes that try to override methods on
 * [sandbox.java.lang.Object] which are specific to the DJVM.
 */
object DisallowSandboxMethods : MemberRule() {
    override fun validate(context: RuleContext, member: Member) = context.validate {
        fail("Class is not allowed to implement toDJVMString()") given (
            member.memberName == "toDJVMString" && member.descriptor == "()Ljava/lang/String;"
        )
        fail("Class is not allowed to implement fromDJVM${member.descriptor}") given (
            member.memberName == FROM_DJVM && member.descriptor.startsWith("()")
        )
    }
}
