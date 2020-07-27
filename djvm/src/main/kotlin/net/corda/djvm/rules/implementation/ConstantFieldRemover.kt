package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.code.impl.toMethodBody
import net.corda.djvm.references.ImmutableMember
import org.objectweb.asm.Opcodes.ACC_STATIC

/**
 * Removes constant objects that are initialised directly in the byte-code.
 * Currently, the only use-case is for re-initialising [String] fields.
 *
 * The primary goal is deleting the type-incompatible [String] constant value
 * from the class's new [sandbox.java.lang.String] field.
 */
object ConstantFieldRemover : MemberDefinitionProvider {

    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember = when {
        isConstantField(member) -> {
            if (member.access and ACC_STATIC != 0) {
                // This may not be needed, because this static field never
                // had a value in the first place! Other classes load this
                // constant value directly from the constant pool instead.
                val mutable = member.toMutable()
                val mapped = mutable.copy(className = context.resolve(member.className))
                mutable.copy(body = listOf(toMethodBody(StringFieldInitializer(mapped)::writeInitializer)), value = null)
            } else {
                member.toMutable().copy(value = null)
            }
        }
        else -> member
    }

    private fun isConstantField(member: ImmutableMember): Boolean {
        return member.value != null && member.descriptor == "Ljava/lang/String;"
    }

    class StringFieldInitializer(private val member: ImmutableMember) {
        fun writeInitializer(emitter: EmitterModuleImpl) = with(emitter) {
            val value = member.value ?: return
            loadConstant(value)
            invokeStatic(DJVM_NAME, "intern", "(Ljava/lang/String;)Lsandbox/java/lang/String;", false)
            putStatic(member.className, member.memberName, "Lsandbox/java/lang/String;")
        }
    }
}