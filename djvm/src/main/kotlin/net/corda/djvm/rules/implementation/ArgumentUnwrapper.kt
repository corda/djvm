package net.corda.djvm.rules.implementation

import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.EMIT_BEFORE_INVOKE
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.FROM_DJVM
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction

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
    override val priority: Int = EMIT_BEFORE_INVOKE

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && context.whitelist.matches(instruction.reference)) {
            fun unwrapString() = invokeStatic("sandbox/java/lang/String", FROM_DJVM, "(Lsandbox/java/lang/String;)Ljava/lang/String;")

            if (hasStringArgument(instruction)) {
                unwrapString()
            } else if (instruction.className == CLASS_NAME) {
                val descriptor = instruction.descriptor
                when {
                    descriptor.startsWith("(Ljava/lang/String;ZLjava/lang/ClassLoader;)") -> {
                        /**
                         * [kotlin.jvm.internal.Intrinsics.checkHasClass] invokes [Class.forName],
                         * so I'm adding support for both of this function's variants. For now.
                         */
                        raiseThirdWordToTop()
                        unwrapString()
                        sinkTopToThirdWord()
                    }
                    descriptor.startsWith("(Ljava/lang/String;[Ljava/lang/Class;)") -> {
                        /**
                         * Support for [Class.getMethod] and [Class.getDeclaredMethod].
                         */
                        swapTopTwoWords()
                        unwrapString()
                        swapTopTwoWords()
                    }
                }
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