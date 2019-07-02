package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import net.corda.djvm.formatting.MemberFormatter
import org.objectweb.asm.Opcodes.*
import sandbox.net.corda.djvm.rules.RuleViolationError

/**
 * The enum-related methods on [Class] all require that enums use [java.lang.Enum]
 * as their super class. So replace their all invocations with ones to equivalent
 * methods on the DJVM class that require [sandbox.java.lang.Enum] instead.
 *
 * The [java.security.ProtectionDomain] object is also untransformable into sandbox
 * objects.
 */
object RewriteClassMethods : Emitter {
    private val memberFormatter = MemberFormatter()

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.owner == "java/lang/Class") {
            when (instruction.operation) {
                INVOKEVIRTUAL -> if (instruction.memberName == "enumConstantDirectory" && instruction.descriptor == "()Ljava/util/Map;") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "enumConstantDirectory",
                        descriptor = "(Ljava/lang/Class;)Lsandbox/java/util/Map;"
                    )
                    preventDefault()
                } else if (instruction.memberName == "isEnum" && instruction.descriptor == "()Z") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "isEnum",
                        descriptor = "(Ljava/lang/Class;)Z"
                    )
                    preventDefault()
                } else if (instruction.memberName == "getEnumConstants" && instruction.descriptor == "()[Ljava/lang/Object;") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "getEnumConstants",
                        descriptor = "(Ljava/lang/Class;)[Ljava/lang/Object;")
                    preventDefault()
                } else if (instruction.memberName == "getProtectionDomain" && instruction.descriptor == "()Ljava/security/ProtectionDomain;") {
                    throwException<RuleViolationError>("Disallowed reference to API; ${memberFormatter.format(instruction.member)}")
                    preventDefault()
                } else if (instruction.memberName == "getClassLoader" && instruction.descriptor == "()Ljava/lang/ClassLoader;") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "getClassLoader",
                        descriptor = "(Ljava/lang/Class;)Ljava/lang/ClassLoader;"
                    )
                    preventDefault()
                }

                INVOKESTATIC -> if (instruction.memberName == "forName") {
                    if (instruction.descriptor == "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;") {
                        invokeStatic(
                            owner = "sandbox/java/lang/DJVM",
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
                            owner = "sandbox/java/lang/DJVM",
                            name = "toSandbox",
                            descriptor = "(Ljava/lang/String;)Ljava/lang/String;"
                        )
                    }
                }
            }
        }
    }
}
