package net.corda.djvm.rules

import net.corda.djvm.code.Instruction
import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.references.ImmutableMember
import net.corda.djvm.validation.RuleContext

/**
 * Representation of a rule that applies to class definitions.
 */
abstract class ClassRule : Rule {

    /**
     * Called when a class definition is visited.
     *
     * @param context The context in which the rule is to be validated.
     * @param clazz The class to apply and validate this rule against.
     */
    abstract fun validate(context: RuleContext, clazz: ImmutableClass)

    final override fun validate(context: RuleContext, clazz: ImmutableClass?, member: ImmutableMember?, instruction: Instruction?) {
        // Only run validation step if applied to the class itself.
        if (clazz != null && member == null && instruction == null) {
            validate(context, clazz)
        }
    }

}
