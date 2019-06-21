package net.corda.djvm.rules.implementation

import net.corda.djvm.code.EmitterModule
import net.corda.djvm.formatting.MemberFormatter
import net.corda.djvm.references.MemberInformation
import sandbox.net.corda.djvm.rules.RuleViolationError

class MemberRuleEnforcer(private val member: MemberInformation) {
    companion object {
        val memberFormatter = MemberFormatter()
    }

    fun forbidNativeMethod(emitter: EmitterModule): Unit = with(emitter) {
        lineNumber(0)
        throwException<RuleViolationError>("Native method has been deleted; ${memberFormatter.format(member)}")
    }

    fun forbidReflection(emitter: EmitterModule): Unit = with(emitter) {
        lineNumber(0)
        throwException<RuleViolationError>("Disallowed reference to reflection API; ${memberFormatter.format(member)}")
    }
}