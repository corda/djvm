package net.corda.djvm.rules.implementation

import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.SANDBOX_CLASS_NAME
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*
import java.util.Collections.unmodifiableMap

/**
 * The enum-related methods on [Class] all require that enums use [java.lang.Enum]
 * as their super class. So replace all their invocations with ones to equivalent
 * methods on the DJVM class that require [sandbox.java.lang.Enum] instead.
 *
 * An annotation must implement [java.lang.annotation.Annotation] and have no other
 * interfaces. This means that the JVM cannot accept anything that implements
 * [sandbox.java.lang.annotation.Annotation] as an annotation! We must therefore
 * redirect the annotation-related methods on [Class] so that the DJVM can perform
 * some mappings.
 */
object RewriteClassMethods : Emitter {
    private const val NO_ARGS = "()"
    private const val CLASS_ARG = "(Ljava/lang/Class;)"

    private val mappedNames: Map<String, String> = unmodifiableMap(mapOf(
        "enumConstantDirectory" to NO_ARGS,
        "getAnnotation" to CLASS_ARG,
        "getAnnotations" to NO_ARGS,
        "getAnnotationsByType" to CLASS_ARG,
        "getCanonicalName" to NO_ARGS,
        "getClassLoader" to NO_ARGS,
        "getDeclaredAnnotation" to CLASS_ARG,
        "getDeclaredAnnotations" to NO_ARGS,
        "getDeclaredAnnotationsByType" to CLASS_ARG,
        "getEnumConstants" to NO_ARGS,
        "getName" to NO_ARGS,
        "getSimpleName" to NO_ARGS,
        "getTypeName" to NO_ARGS,
        "isAnnotationPresent" to CLASS_ARG,
        "isEnum" to NO_ARGS,
        "toGenericString" to NO_ARGS,
        "toString" to NO_ARGS
    ))

    private fun prependClassArgTo(descriptor: String): String {
        return "(L$CLASS_NAME;${descriptor.substring(1)}"
    }

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == CLASS_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    mappedNames[instruction.memberName]?.also { argTypes ->
                        val descriptor = instruction.descriptor
                        if (descriptor.startsWith(argTypes)) {
                            invokeStatic(
                                owner = SANDBOX_CLASS_NAME,
                                name = instruction.memberName,
                                descriptor = prependClassArgTo(context.resolveDescriptor(descriptor))
                            )
                            preventDefault()
                        }
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
