@file:JvmName("JavaAnnotationConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.references.Member
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_PUBLIC

/**
 * Generate [Member] objects that will be stitched into [sandbox.java.lang.annotation.Annotation].
 */
fun generateJavaAnnotationMethods(): List<Member> = listOf(
    MethodBuilder(
        access = ACC_PUBLIC or ACC_ABSTRACT,
        className = sandboxed(Annotation::class.java),
        memberName = "toString",
        descriptor = "()Ljava/lang/String;"
    ).build(),

    object : MethodBuilder(
        access = ACC_PUBLIC,
        className = sandboxed(Annotation::class.java),
        memberName = "jvmAnnotation",
        descriptor = "()Ljava/lang/annotation/Annotation;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushNull()
            returnObject()
        }
    }.withBody().build()
)
