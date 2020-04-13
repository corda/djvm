package net.corda.djvm.rules.implementation

import net.corda.djvm.code.ANNOTATED_ELEMENT_NAME
import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
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
    private const val NO_ARG = "()"
    private const val CLASS_ARG = "(Ljava/lang/Class;)"

    private val mappedNames: Map<String, Array<String>> = unmodifiableMap(mapOf(
        "enumConstantDirectory" to arrayOf(NO_ARG, CLASS_NAME),
        "getAnnotation" to arrayOf(CLASS_ARG, ANNOTATED_ELEMENT_NAME),
        "getAnnotations" to arrayOf(NO_ARG, ANNOTATED_ELEMENT_NAME),
        "getAnnotationsByType" to arrayOf(CLASS_ARG, ANNOTATED_ELEMENT_NAME),
        "getCanonicalName" to arrayOf(NO_ARG, CLASS_NAME),
        "getClassLoader" to arrayOf(NO_ARG, CLASS_NAME),
        "getDeclaredAnnotation" to arrayOf(CLASS_ARG, ANNOTATED_ELEMENT_NAME),
        "getDeclaredAnnotations" to arrayOf(NO_ARG, ANNOTATED_ELEMENT_NAME),
        "getDeclaredAnnotationsByType" to arrayOf(CLASS_ARG, ANNOTATED_ELEMENT_NAME),
        "getEnumConstants" to arrayOf(NO_ARG, CLASS_NAME),
        "getName" to arrayOf(NO_ARG, CLASS_NAME),
        "getSimpleName" to arrayOf(NO_ARG, CLASS_NAME),
        "getTypeName" to arrayOf(NO_ARG, CLASS_NAME),
        "isAnnotationPresent" to arrayOf(CLASS_ARG, ANNOTATED_ELEMENT_NAME),
        "isEnum" to arrayOf(NO_ARG, CLASS_NAME),
        "toGenericString" to arrayOf(NO_ARG, CLASS_NAME),
        "toString" to arrayOf(NO_ARG, CLASS_NAME)
    ))

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == CLASS_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    mappedNames[instruction.memberName]?.also { mapping ->
                        val descriptor = instruction.descriptor
                        if (descriptor.startsWith(mapping[0])) {
                            val returnTypeIdx = descriptor.indexOf(')') + 1
                            val newReturnType = context.resolveDescriptor(descriptor.substring(returnTypeIdx))
                            invokeStatic(
                                owner = DJVM_NAME,
                                name = instruction.memberName,
                                descriptor = "(L${mapping[1]};${descriptor.substring(1, returnTypeIdx)}$newReturnType"
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
