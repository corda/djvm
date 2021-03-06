@file:JvmName("JavaMathConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.impl.CONSTRUCTOR_NAME
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.code.impl.FROM_DJVM
import net.corda.djvm.references.Member

/**
 * Generate [Member] objects that will be stitched into [sandbox.java.math.BigInteger]
 * and [sandbox.java.math.BigDecimal].
 */
fun generateJavaMathMethods(): List<Member> = object : FromDJVMBuilder(
    className = sandboxed(java.math.BigInteger::class.java),
    bridgeDescriptor = "()Ljava/math/BigInteger;"
) {
    /**
     * Implements `BigInteger.fromDJVM()`:
     * ```
     *     return new java.math.BigInteger(signum(), toByteArray())
     * ```
     */
    override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
        new("java/math/BigInteger")
        duplicate()
        pushObject(0)
        invokeVirtual(className, "signum", "()I")
        pushObject(0)
        invokeVirtual(className, "toByteArray", "()[B")
        invokeSpecial("java/math/BigInteger", CONSTRUCTOR_NAME, "(I[B)V")
        returnObject()
    }
}.build() + object : FromDJVMBuilder(
    className = sandboxed(java.math.BigDecimal::class.java),
    bridgeDescriptor = "()Ljava/math/BigDecimal;"
) {
    /**
     * Implements `BigDecimal.fromDJVM()`:
     * ```
     *     return new java.math.BigDecimal(unscaledValue().fromDJVM(), scale())
     * ```
     */
    override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
        new("java/math/BigDecimal")
        duplicate()
        pushObject(0)
        invokeVirtual(className, "unscaledValue", "()Lsandbox/java/math/BigInteger;")
        invokeVirtual("sandbox/java/math/BigInteger", FROM_DJVM, "()Ljava/math/BigInteger;")
        pushObject(0)
        invokeVirtual(className, "scale", "()I")
        invokeSpecial("java/math/BigDecimal", CONSTRUCTOR_NAME, "(Ljava/math/BigInteger;I)V")
        returnObject()
    }
}.build()
