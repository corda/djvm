package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.DynamicInvocationInstruction
import net.corda.djvm.rules.InstructionRule
import net.corda.djvm.validation.RuleContext
import org.objectweb.asm.Handle

/**
 * Rule that checks for invalid dynamic invocations.
 */
object DisallowDynamicInvocation : InstructionRule() {

    override fun validate(context: RuleContext, instruction: Instruction) = context.validate {
        if (instruction is DynamicInvocationInstruction && !isJvmLambda(instruction.bootstrapArgs)) {
            warn("Disallowed dynamic invocation in ${formatFor(instruction.method)}").always()
            // TODO Allow specific lambda and string concatenation meta-factories used by Java code itself
        }
    }

    private fun isJvmLambda(bootstrapArgs: Array<out Any?>): Boolean {
        return (bootstrapArgs.size > 1) && isJvmInternal((bootstrapArgs[1] as? Handle ?: return false).owner)
    }

    private fun isJvmInternal(className: String): Boolean {
        return className.startsWith("java/")
    }
}
