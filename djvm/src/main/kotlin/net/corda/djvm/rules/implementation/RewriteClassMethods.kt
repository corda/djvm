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
class RewriteClassMethods : Emitter {
    private val memberFormatter = MemberFormatter()

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.owner == "java/lang/Class") {
            when (instruction.operation) {
                INVOKEVIRTUAL -> if (instruction.memberName == "enumConstantDirectory" && instruction.signature == "()Ljava/util/Map;") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "enumConstantDirectory",
                        descriptor = "(Ljava/lang/Class;)Lsandbox/java/util/Map;"
                    )
                    preventDefault()
                } else if (instruction.memberName == "isEnum" && instruction.signature == "()Z") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "isEnum",
                        descriptor = "(Ljava/lang/Class;)Z"
                    )
                    preventDefault()
                } else if (instruction.memberName == "getEnumConstants" && instruction.signature == "()[Ljava/lang/Object;") {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "getEnumConstants",
                        descriptor = "(Ljava/lang/Class;)[Ljava/lang/Object;")
                    preventDefault()
                } else if (instruction.memberName == "getProtectionDomain" && instruction.signature == "()Ljava/security/ProtectionDomain;") {
                    throwException<RuleViolationError>("Disallowed reference to API; ${memberFormatter.format(instruction.member)}")
                    preventDefault()
                }

                INVOKESTATIC -> if (isClassForName(instruction)) {
                    invokeStatic(
                        owner = "sandbox/java/lang/DJVM",
                        name = "classForName",
                        descriptor = instruction.signature
                    )
                    preventDefault()
                }
            }
        }
    }

    private fun isClassForName(instruction: MemberAccessInstruction): Boolean
        = instruction.memberName == "forName" &&
            (instruction.signature == "(Ljava/lang/String;)Ljava/lang/Class;" ||
                    instruction.signature == "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")
}
