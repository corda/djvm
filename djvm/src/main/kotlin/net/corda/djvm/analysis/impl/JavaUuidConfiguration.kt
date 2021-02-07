@file:JvmName("JavaUuidConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.impl.CONSTRUCTOR_NAME
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.references.Member
import java.util.*

/**
 * Generate [Member] objects that will be stitched into [UUID].
 */
fun generateJavaUuidMethods(): List<Member> = object : FromDJVMBuilder(
    className = sandboxed(UUID::class.java),
    bridgeDescriptor = "()Ljava/util/UUID;"
) {
    /**
     * Implements `UUID.fromDJVM()`:
     * ```
     *     return new java.util.UUID(mostSigBits, leastSigBits)
     * ```
     */
    override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
        new("java/util/UUID")
        duplicate()
        pushObject(0)
        pushField(className, "mostSigBits", "J")
        pushObject(0)
        pushField(className, "leastSigBits", "J")
        invokeSpecial("java/util/UUID", CONSTRUCTOR_NAME, "(JJ)V")
        returnObject()
    }
}.build()
