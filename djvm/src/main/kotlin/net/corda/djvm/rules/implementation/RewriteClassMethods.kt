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
    private val mappedNames = unmodifiableSet(setOf(
        "enumConstantDirectory",
        "getAnnotation",
        "getAnnotations",
        "getAnnotationsByType",
        "getCanonicalName",
        "getClassLoader",
        "getDeclaredAnnotation",
        "getDeclaredAnnotations",
        "getDeclaredAnnotationsByType",
        "getEnumConstants",
        "getName",
        "getSimpleName",
        "getTypeName",
        "isAnnotationPresent",
        "isEnum",
        "toGenericString",
        "toString"
    ))

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == CLASS_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    if (instruction.memberName in mappedNames){
                        val descriptor = instruction.descriptor
                        val returnTypeIdx = descriptor.indexOf(')') + 1
                        val newReturnType = context.resolveDescriptor(descriptor.substring(returnTypeIdx))
                        invokeStatic(
                            owner = DJVM_NAME,
                            name = instruction.memberName,
                            descriptor = "(L$CLASS_NAME;${descriptor.substring(1, returnTypeIdx)}$newReturnType"
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
