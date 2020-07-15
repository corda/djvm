package net.corda.djvm.rules.implementation

import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.rules.ClassRule
import net.corda.djvm.validation.RuleContext

/**
 * Disallow loading of classes that have been defined in the 'sandbox' root package.
 */
object DisallowOverriddenSandboxPackage : ClassRule() {

    override fun validate(context: RuleContext, clazz: ImmutableClass) = context.validate {
        fail("Cannot load class explicitly defined in the 'sandbox' root package; ${clazz.name}") given
                isSandboxClass(clazz.name)
    }

}
