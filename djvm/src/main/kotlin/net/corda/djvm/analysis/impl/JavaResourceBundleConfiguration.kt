@file:JvmName("JavaResourceBundleConfiguration")
package net.corda.djvm.analysis.impl

import net.corda.djvm.analysis.AnalysisConfiguration.Companion.sandboxed
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.references.Member
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import java.util.*

private const val GET_BUNDLE = "getBundle"

/**
 * Generate [Member] objects for the [sandbox.java.lang.Object.fromDJVM]
 * methods that will be stitched into the [sandbox.java.util.ResourceBundle] class.
 */
fun generateJavaResourceBundleMethods(): List<Member> = listOf(
    /**
     * Redirect the [ResourceBundle] handling.
     */
    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;)Lsandbox/java/util/ResourceBundle;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushDefaultLocale()
            pushDefaultControl()
            returnResourceBundle()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushDefaultLocale()
            pushObject(1)
            returnResourceBundle()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;)Lsandbox/java/util/ResourceBundle;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushObject(1)
            pushDefaultControl()
            returnResourceBundle()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushObject(1)
            pushObject(2)
            returnResourceBundle()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Ljava/lang/ClassLoader;)Lsandbox/java/util/ResourceBundle;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushObject(1)
            pushDefaultControl()
            returnResourceBundle()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC or ACC_STATIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Ljava/lang/ClassLoader;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
    ) {
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushObject(1)
            pushObject(3)
            returnResourceBundle()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = "childOf",
        descriptor = "(Lsandbox/java/util/ResourceBundle;)V"
    ) {
        /**
         * Implements `ResourceBundle.childOf(ResourceBundle parent)`:
         * ```
         *     if (this.parent == null) {
         *         this.parent = parent
         *     }
         *     return
         * ```
         */
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            lineNumber(1)
            pushObject(0)
            pushField(className, "parent", "Lsandbox/java/util/ResourceBundle;")

            val doEnd = Label()
            jump(IFNONNULL, doEnd)
            pushObject(0)
            pushObject(1)
            popField(className, "parent", "Lsandbox/java/util/ResourceBundle;")

            lineNumber(2, doEnd)
            returnVoid()
        }
    }.withBody()
     .build(),

    object : MethodBuilder(
        access = ACC_PUBLIC,
        className = sandboxed(ResourceBundle::class.java),
        memberName = "init",
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;)V"
    ) {
        /**
         * Implements `ResourceBundle.init(String name, Locale locale)`:
         * ```
         *     this.name = name
         *     this.locale = locale
         *     return
         * ```
         */
        override fun writeBody(emitter: EmitterModuleImpl) = with(emitter) {
            pushObject(0)
            pushObject(1)
            popField(className, "name", "Lsandbox/java/lang/String;")

            pushObject(0)
            pushObject(2)
            popField(className, "locale", "Lsandbox/java/util/Locale;")

            returnVoid()
        }
    }.withBody()
    .build()
)

private fun EmitterModuleImpl.returnResourceBundle() {
    invokeStatic(
        owner = DJVM_NAME,
        name = GET_BUNDLE,
        descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
    )
    returnObject()
}

private fun EmitterModuleImpl.pushDefaultLocale() {
    invokeStatic(
        owner = sandboxed(Locale::class.java),
        name = "getDefault",
        descriptor = "()Lsandbox/java/util/Locale;"
    )
}

private fun EmitterModuleImpl.pushDefaultControl() {
    /**
     * The baseName parameter is expected already to have been
     * pushed onto the stack, just below the [Locale] value, so
     * emit instructions to rearrange the stack as follows:
     * ```
     *     [W1]    [W2]
     *     [W2] -> [W1]
     *             [W2]
     * ```
     */
    instruction(DUP2)
    pop()
    invokeStatic(
        owner = sandboxed(ResourceBundle::class.java),
        name = "getDefaultControl",
        descriptor = "(Lsandbox/java/lang/String;)Lsandbox/java/util/ResourceBundle\$Control;"
    )
}
