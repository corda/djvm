package net.corda.djvm.rules.implementation

import net.corda.djvm.code.CLASSLOADER_NAME
import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.EMIT_BEFORE_INVOKE
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.FROM_DJVM
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import java.util.Collections.unmodifiableSet

/**
 * Some whitelisted functions have [java.lang.String] arguments, so we
 * need to unwrap the [sandbox.java.lang.String] object before invoking.
 *
 * [java.security.AccessController.doPrivileged] actions also need to
 * be converted into a form that the JVM can handle.
 *
 * There are lots of rabbits in this hole because method arguments are
 * theoretically arbitrary. However, in practice WE control the whitelist.
 */
object ArgumentUnwrapper : Emitter {
    private val THUNKED_CLASSES = unmodifiableSet(setOf(CLASS_NAME, CLASSLOADER_NAME))

    override val priority: Int = EMIT_BEFORE_INVOKE

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction
            && context.whitelist.matches(instruction.reference)
            && instruction.className !in THUNKED_CLASSES
        ) {
            fun unwrapString() = invokeStatic("sandbox/java/lang/String", FROM_DJVM, "(Lsandbox/java/lang/String;)Ljava/lang/String;")

            if (hasStringArgument(instruction)) {
                unwrapString()
            } else if (instruction.className == "java/security/AccessController") {
                val descriptor = instruction.descriptor
                when {
                    descriptor.startsWith("(Ljava/security/PrivilegedAction;)") -> {
                        invokeStatic(
                            owner = "sandbox/java/security/DJVM",
                            name = FROM_DJVM,
                            descriptor = "(Lsandbox/java/security/PrivilegedAction;)Ljava/security/PrivilegedAction;"
                        )
                    }

                    descriptor.startsWith("(Ljava/security/PrivilegedExceptionAction;)") -> {
                        invokeStatic(
                            owner = "sandbox/java/security/DJVM",
                            name = FROM_DJVM,
                            descriptor = "(Lsandbox/java/security/PrivilegedExceptionAction;)Ljava/security/PrivilegedExceptionAction;"
                        )
                    }
                }
            }
        }
    }

    private fun hasStringArgument(method: MemberAccessInstruction) = method.descriptor.contains("Ljava/lang/String;)")
}