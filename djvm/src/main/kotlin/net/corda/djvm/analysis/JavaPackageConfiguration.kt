@file:JvmName("JavaPackageConfiguration")
package net.corda.djvm.analysis

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.references.Member
import org.objectweb.asm.Opcodes.*

/**
 * Generate [Member] objects that will be stitched into [Package].
 */
fun generateJavaPackageMethods(): List<Member> = listOf(
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(java.lang.Package::class.java),
        memberName = "getPackage",
        descriptor = "(Lsandbox/java/lang/String;)Lsandbox/java/lang/Package;"
    ) {
        /**
         * Disable Package.getPackage(String).
         *     return null
         */
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushNull()
            returnObject()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(java.lang.Package::class.java),
        memberName = "getPackages",
        descriptor = "()[Lsandbox/java/lang/Package;"
    ) {
        /**
         * Disable Package.getPackages().
         *     return new Package[0]
         */
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushIntegerZero()
            new(className, ANEWARRAY)
            returnObject()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_STATIC,
        className = sandboxed(java.lang.Package::class.java),
        memberName = "getPackage",
        descriptor = "(Ljava/lang/Class;)Lsandbox/java/lang/Package;",
        signature = "(Ljava/lang/Class<*>;)Lsandbox/java/lang/Package;"
    ) {
        /**
         * Disable Package.getPackage(Class<?>).
         *     return null
         */
        override fun writeBody(emitter: EmitterModule) = with(emitter) {
            pushNull()
            returnObject()
        }
    }.withBody()
     .build()
)