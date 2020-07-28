package net.corda.djvm.rules.implementation

import net.corda.djvm.analysis.AnalysisRuntimeContext
import net.corda.djvm.code.MemberDefinitionProvider
import net.corda.djvm.references.ImmutableMember

/**
 * Removes constant objects that are initialised directly in the byte-code.
 * Currently, the only use-case is for re-initialising [String] fields.
 *
 * The primary goal is deleting the type-incompatible [String] constant value
 * from the class's new [sandbox.java.lang.String] field.
 */
object ConstantFieldRemover : MemberDefinitionProvider {

    override fun define(context: AnalysisRuntimeContext, member: ImmutableMember): ImmutableMember = when {
        // We don't need this value at run-time because this static
        // final field is never accessed. Other classes load this
        // constant value directly from the constant pool instead.
        isConstantField(member) -> member.toMutable().copy(value = null)
        else -> member
    }

    private fun isConstantField(member: ImmutableMember): Boolean {
        return member.value != null && member.descriptor == "Ljava/lang/String;"
    }
}