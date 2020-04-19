package net.corda.djvm.rules.implementation

import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*
import java.util.Collections.unmodifiableSet

/**
 * The enum-related methods on [Class] all require that enums use [java.lang.Enum]
 * as their super class. So replace their all invocations with ones to equivalent
 * methods on the DJVM class that require [sandbox.java.lang.Enum] instead.
 *
 * The [java.security.ProtectionDomain] object is also untransformable into sandbox
 * objects.
 */
object RewriteClassMethods : Emitter {
    private val mappedNames = unmodifiableSet(setOf(
        "enumConstantDirectory",
        "getCanonicalName",
        "getClassLoader",
        "getEnumConstants",
        "getName",
        "getSimpleName",
        "getTypeName",
        "isEnum",
        "toGenericString",
        "toString"
    ))

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == CLASS_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    if (instruction.memberName in mappedNames && instruction.descriptor.startsWith("()")) {
                        invokeStatic(
                            owner = DJVM_NAME,
                            name = instruction.memberName,
                            descriptor = "(Ljava/lang/Class;)"
                                + context.resolveDescriptor(instruction.descriptor.drop("()".length))
                        )
                        preventDefault()
                    }

                INVOKESTATIC ->
                    if (instruction.memberName == "forName") {
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
