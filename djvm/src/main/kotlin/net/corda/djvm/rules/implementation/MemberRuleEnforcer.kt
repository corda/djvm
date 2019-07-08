package net.corda.djvm.rules.implementation

import net.corda.djvm.code.EmitterModule
import net.corda.djvm.formatting.MemberFormatter
import net.corda.djvm.references.MemberInformation

class MemberRuleEnforcer(private val member: MemberInformation) {
    companion object {
        val memberFormatter = MemberFormatter()
    }

    fun forbidNativeMethod(emitter: EmitterModule): Unit = with(emitter) {
        lineNumber(0)
        throwRuleViolationError("Native method has been deleted; ${memberFormatter.format(member)}")
    }

    fun forbidReflection(emitter: EmitterModule): Unit = with(emitter) {
        lineNumber(0)
        throwRuleViolationError("Disallowed reference to reflection API; ${memberFormatter.format(member)}")
    }
}