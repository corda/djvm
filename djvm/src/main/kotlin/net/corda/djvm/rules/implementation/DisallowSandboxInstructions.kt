package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import net.corda.djvm.rules.InstructionRule
import net.corda.djvm.rules.implementation.MemberRuleEnforcer.Companion.formatFor
import net.corda.djvm.validation.RuleContext

/**
 * Disallow loading of classes that try to access methods and fields
 * in the sandbox.* namespace before the DJVM has instrumented them.
 */
object DisallowSandboxInstructions : InstructionRule() {

    override fun validate(context: RuleContext, instruction: Instruction) = context.validate {
        if (instruction is MemberAccessInstruction && isSandboxClass(instruction.className)) {
            fail("Access to ${formatFor(instruction)} is forbidden.").always()
        }
    }
}
