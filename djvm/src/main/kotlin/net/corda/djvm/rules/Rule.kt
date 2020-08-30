package net.corda.djvm.rules

import net.corda.djvm.code.Instruction
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.validation.impl.RuleContext

/**
 * Representation of a rule.
 */
@FunctionalInterface
interface Rule {

    /**
     * Validate class, member and/or instruction in the provided context.
     *
     * @param context The context in which the rule is to be validated.
     * @param clazz The class to apply and validate this rule against, if any.
     * @param member The class member to apply and validate this rule against, if any.
     * @param instruction The instruction to apply and validate this rule against, if any.
     */
    fun validate(context: RuleContext, clazz: ImmutableClass?, member: ImmutableMember?, instruction: Instruction?)

}
