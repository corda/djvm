@file:JvmName("JavaEnumConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.references.Member

fun generateJavaEnumMethods(): List<Member> = object : FromDJVMBuilder(
    className = AnalysisConfiguration.sandboxed(Enum::class.java),
    bridgeDescriptor = "()Ljava/lang/Enum;",
    signature = "()Ljava/lang/Enum<*>;"
) {
    override fun writeBody(emitter: EmitterModule) = with(emitter) {
        pushObject(0)
        invokeStatic(DJVM_NAME, "fromDJVMEnum", "(Lsandbox/java/lang/Enum;)Ljava/lang/Enum;")
        returnObject()
    }
}.build()