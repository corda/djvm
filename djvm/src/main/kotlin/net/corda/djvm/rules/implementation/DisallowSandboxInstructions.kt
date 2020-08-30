package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import net.corda.djvm.code.instructions.TypeInstruction
import net.corda.djvm.rules.InstructionRule
import net.corda.djvm.validation.impl.RuleContext
import org.objectweb.asm.Type

/**
 * Disallow loading of classes that try to access methods,fields and types
 * in the sandbox.* namespace before the DJVM has instrumented them.
 */
object DisallowSandboxInstructions : InstructionRule() {

    override fun validate(context: RuleContext, instruction: Instruction) = context.validate {
        when {
            instruction is MemberAccessInstruction && isSandboxClass(instruction.className) ->
                fail("Access to ${formatFor(instruction)} is forbidden.").always()

            instruction is TypeInstruction && isSandboxClass(instruction.typeName) ->
                fail("Casting to ${Type.getObjectType(instruction.typeName).className} is forbidden.").always()
        }
    }
}
