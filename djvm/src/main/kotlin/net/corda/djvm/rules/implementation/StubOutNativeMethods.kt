package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.code.impl.toMethodBody
import net.corda.djvm.references.ImmutableMember
import org.objectweb.asm.Opcodes.*
import java.lang.reflect.Modifier

/**
 * Rule that replaces a native method with a stub that throws an exception.
 */
object StubOutNativeMethods : MemberDefinitionProvider {

    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember = when {
        member.isMethod && isNative(member) -> member.toMutable().copy(
            access = member.access and ACC_NATIVE.inv(),
            body = member.body + toMethodBody(
                if (isForStubbing(member)) {
                    ::writeStubMethodBody
                } else {
                    MemberRuleEnforcer(member)::forbidNativeMethod
                }
            )
        )
        else -> member
    }

    private fun writeStubMethodBody(emitter: EmitterModuleImpl): Unit = with(emitter) {
        returnVoid()
    }

    private fun isForStubbing(member: ImmutableMember): Boolean = member.descriptor == "()V" && member.memberName == "registerNatives"

    private fun isNative(member: ImmutableMember): Boolean = Modifier.isNative(member.access)
}
