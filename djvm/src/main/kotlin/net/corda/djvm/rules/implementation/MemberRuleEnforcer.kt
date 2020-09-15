package net.corda.djvm.rules.implementation

import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.references.MemberInformation

class MemberRuleEnforcer(private val member: MemberInformation) {
    fun forbidNativeMethod(emitter: EmitterModuleImpl) = with(emitter) {
        lineNumber(0)
        throwRuleViolationError("Native method has been deleted; ${formatFor(member)}")
    }

    fun forbidAPI(emitter: EmitterModuleImpl) = with(emitter) {
        lineNumber(0)
        throwRuleViolationError("Disallowed reference to API; ${formatFor(member)}")
    }
}