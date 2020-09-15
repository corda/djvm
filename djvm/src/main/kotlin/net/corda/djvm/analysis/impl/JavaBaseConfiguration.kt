@file:JvmName("JavaBaseConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.impl.CONSTRUCTOR_NAME
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.references.Member
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.IFNONNULL
import java.io.InputStream
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.Security
import javax.security.auth.x500.X500Principal
import javax.xml.datatype.DatatypeFactory

const val X500_NAME = "sandbox/sun/security/x509/X500Name"

fun generateJavaBaseMethods(): List<Member> = listOf(
    object : MethodBuilder(
        access = ACC_STATIC or ACC_PRIVATE,
        className = sandboxed(Charset::class.java),
        memberName = "providers",
        descriptor = "()Lsandbox/java/util/Iterator;",
        signature = "()Lsandbox/java/util/Iterator<Lsandbox/java/nio/charset/spi/CharsetProvider;>;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            invokeStatic("sandbox/java/util/Collections", "emptyIterator", descriptor)
            returnObject()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_STATIC or ACC_PRIVATE,
        className = sandboxed(Security::class.java),
        memberName = "initialize",
        descriptor = "()V"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            invokeStatic(DJVM_NAME, "getSecurityProviders", "()Lsandbox/java/util/Properties;")
            putStatic(className, "props", "Lsandbox/java/util/Properties;")
            returnVoid()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PRIVATE or ACC_STATIC,
        className = sandboxed(SecureRandom::class.java),
        memberName = "getPrngAlgorithm",
        descriptor = "()Lsandbox/java/lang/String;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushNull()
            returnObject()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PRIVATE or ACC_STATIC,
        className = "sandbox/sun/util/locale/provider/JRELocaleProviderAdapter",
        memberName = "isNonENLangSupported",
        descriptor = "()Z"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushFalse()
            returnInteger()
        }
    }.withBody()
     .build(),

    // Create factory function to wrap java.io.InputStream.
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(InputStream::class.java),
        memberName = "toDJVM",
        descriptor = "(Ljava/io/InputStream;)Lsandbox/java/io/InputStream;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            val doWrap = Label()
            lineNumber(1)
            pushObject(0)
            jump(IFNONNULL, doWrap)
            pushNull()
            returnObject()

            lineNumber(2, doWrap)
            new("sandbox/java/io/DJVMInputStream")
            duplicate()
            pushObject(0)
            invokeSpecial("sandbox/java/io/DJVMInputStream", CONSTRUCTOR_NAME, "(Ljava/io/InputStream;)V")
            returnObject()
        }
    }.withBody()
     .build(),

    /**
     * Create [sandbox.javax.security.auth.x500.X500Principal.unwrap] method
     * to expose existing private [X500Principal.thisX500Name] field.
     */
    object : MethodBuilder(
        access = ACC_FINAL,
        className = sandboxed(X500Principal::class.java),
        memberName = "unwrap",
        descriptor = "()Lsandbox/sun/security/x509/X500Name;"
    ) {
        /**
         * Implement package private accessor.
         *     return thisX500Name
         */
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushField(className, "thisX500Name", "Lsandbox/sun/security/x509/X500Name;")
            returnObject()
        }
    }.withBody()
     .build(),

    /**
     * Reimplement these methods so that they don't require reflection.
     */
    object : MethodBuilder(
        access = ACC_PUBLIC,
        className = X500_NAME,
        memberName = "asX500Principal",
        descriptor = "()Lsandbox/javax/security/auth/x500/X500Principal;"
    ) {
        /**
         * Reimplement [sandbox.sun.security.x509.X500Name.asX500Principal] without reflection.
         *     return DJVM.create(this)
         */
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            invokeStatic(
                owner = "sandbox/javax/security/auth/x500/DJVM",
                name = "create",
                descriptor = "(Lsandbox/sun/security/x509/X500Name;)Lsandbox/javax/security/auth/x500/X500Principal;"
            )
            returnObject()
        }
    }.withBody()
     .build(),
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = X500_NAME,
        memberName = "asX500Name",
        descriptor = "(Lsandbox/javax/security/auth/x500/X500Principal;)Lsandbox/sun/security/x509/X500Name;"
    ) {
        /**
         * Reimplement [sandbox.sun.security.x509.X500Name.asX500Name] without reflection.
         *     X500Name name = DJVM.unwrap(principal)
         *     name.x500Principal = principal
         *     return name
         */
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            invokeStatic(
                owner = "sandbox/javax/security/auth/x500/DJVM",
                name = "unwrap",
                descriptor = "(Lsandbox/javax/security/auth/x500/X500Principal;)Lsandbox/sun/security/x509/X500Name;"
            )
            popObject(1)
            pushObject(1)
            pushObject(0)
            popField(
                owner = className,
                name = "x500Principal",
                descriptor = "Lsandbox/javax/security/auth/x500/X500Principal;"
            )
            pushObject(1)
            returnObject()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(DatatypeFactory::class.java),
        memberName = "newInstance",
        descriptor = "()Lsandbox/javax/xml/datatype/DatatypeFactory;"
    ) {
        /**
         * Reimplement [DatatypeFactory.newInstance] to use the JDK's basic implementation.
         *     return new com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl()
         */
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            val implementationClass = "sandbox/com/sun/org/apache/xerces/internal/jaxp/datatype/DatatypeFactoryImpl"
            new(implementationClass)
            duplicate()
            invokeSpecial(implementationClass, CONSTRUCTOR_NAME, "()V")
            returnObject()
        }
    }.withBody()
     .build()
)
