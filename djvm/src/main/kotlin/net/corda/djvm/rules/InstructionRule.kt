package net.corda.djvm.rules

import net.corda.djvm.code.Instruction
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.validation.RuleContext

/**
 * Representation of a rule that applies to byte code instructions.
 */
abstract class InstructionRule : Rule {

    /**
     * Called when an instruction is visited.
     *
     * @param context The context in which the rule is to be validated.
     * @param instruction The instruction to apply and validate this rule against.
     */
    abstract fun validate(context: RuleContext, instruction: Instruction)

    final override fun validate(context: RuleContext, clazz: ImmutableClass?, member: ImmutableMember?, instruction: Instruction?) {
        // Only run validation step if applied to the class member itself.
        if (clazz != null && member != null && instruction != null) {
            validate(context, instruction)
        }
    }

}
