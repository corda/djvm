package net.corda.djvm.rules.implementation

import net.corda.djvm.references.ImmutableClass
import net.corda.djvm.rules.ClassRule
import net.corda.djvm.validation.impl.RuleContext
import org.objectweb.asm.Opcodes.*

/**
 * Rule that checks for classes compiled for unsupported API versions.
 */
object DisallowUnsupportedApiVersions : ClassRule() {

    override fun validate(context: RuleContext, clazz: ImmutableClass) = context.validate {
        fail("Unsupported Java API version '${versionString(clazz.apiVersion)}'") given
                (clazz.apiVersion !in supportedVersions)
    }

    private val supportedVersions = IntRange(V1_2, V1_8)

    private val versionMap = mapOf(
        V1_1 to "1.1", V1_2 to "1.2", V1_3 to "1.3", V1_4 to "1.4",
        V1_5 to "1.5", V1_6 to "1.6", V1_7 to "1.7", V1_8 to "1.8",
        V9 to "9", V10 to "10", V11 to "11", V12 to "12", V13 to "13",
        V14 to "14", V15 to "15"
    )

    private fun versionString(version: Int) = versionMap.getOrDefault(version, "unknown")
}
