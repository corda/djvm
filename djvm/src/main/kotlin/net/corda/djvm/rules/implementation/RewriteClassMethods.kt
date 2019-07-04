package net.corda.djvm.rules.implementation

import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import net.corda.djvm.rules.implementation.MemberRuleEnforcer.Companion.formatFor
import org.objectweb.asm.Opcodes.*

/**
 * The enum-related methods on [Class] all require that enums use [java.lang.Enum]
 * as their super class. So replace their all invocations with ones to equivalent
 * methods on the DJVM class that require [sandbox.java.lang.Enum] instead.
 *
 * The [java.security.ProtectionDomain] object is also untransformable into sandbox
 * objects.
 */
object RewriteClassMethods : Emitter {

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == "java/lang/Class") {
            when (instruction.operation) {
                INVOKEVIRTUAL -> if (instruction.memberName == "enumConstantDirectory" && instruction.descriptor == "()Ljava/util/Map;") {
                    invokeStatic(
                        owner = DJVM_NAME,
                        name = "enumConstantDirectory",
                        descriptor = "(Ljava/lang/Class;)Lsandbox/java/util/Map;"
                    )
                    preventDefault()
                } else if (instruction.memberName == "isEnum" && instruction.descriptor == "()Z") {
                    invokeStatic(
                        owner = DJVM_NAME,
                        name = "isEnum",
                        descriptor = "(Ljava/lang/Class;)Z"
                    )
                    preventDefault()
                } else if (instruction.memberName == "getEnumConstants" && instruction.descriptor == "()[Ljava/lang/Object;") {
                    invokeStatic(
                        owner = DJVM_NAME,
                        name = "getEnumConstants",
                        descriptor = "(Ljava/lang/Class;)[Ljava/lang/Object;")
                    preventDefault()
                } else if (instruction.memberName == "getProtectionDomain" && instruction.descriptor == "()Ljava/security/ProtectionDomain;") {
                    throwRuleViolationError("Disallowed reference to API; ${formatFor(instruction)}")
                    preventDefault()
                } else if (instruction.memberName == "getClassLoader" && instruction.descriptor == "()Ljava/lang/ClassLoader;") {
                    invokeStatic(
                        owner = DJVM_NAME,
                        name = "getClassLoader",
                        descriptor = "(Ljava/lang/Class;)Ljava/lang/ClassLoader;"
                    )
                    preventDefault()
                }

                INVOKESTATIC -> if (instruction.memberName == "forName") {
                    if (instruction.descriptor == "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;") {
                        invokeStatic(
                            owner = DJVM_NAME,
                            name = "classForName",
                            descriptor = instruction.descriptor
                        )
                        preventDefault()
                    } else if (instruction.descriptor == "(Ljava/lang/String;)Ljava/lang/Class;") {
                        // Map the class name into the sandbox namespace, but still invoke
                        // Class.forName(String) here so that it uses the caller's classloader
                        // and not the classloader of the DJVM class. We cannot assume that
                        // the DJVM class has access to the user's libraries.
                        invokeStatic(
                            owner = DJVM_NAME,
                            name = "toSandbox",
                            descriptor = "(Ljava/lang/String;)Ljava/lang/String;"
                        )
                    }
                }
            }
        }
    }
}
