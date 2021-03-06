package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.impl.CLASS_NAME
import net.corda.djvm.code.impl.SANDBOX_CLASS_NAME
import net.corda.djvm.code.impl.emit
import net.corda.djvm.code.impl.isClassStaticThunk
import net.corda.djvm.code.impl.isClassVirtualThunk
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

/**
 * Methods like [Class.getName] return [java.lang.String], but we need them to
 * return [sandbox.java.lang.String] instead. Redirect their invocations to
 * "thunks" on [sandbox.java.lang.DJVMClass] that wrap the return value for us.
 * We do something similar for method references like "Class::getName" too; see
 * [net.corda.djvm.code.impl.SandboxRemapper.mapValue].
 *
 * The enum-related methods on [Class] all require that enums use [java.lang.Enum]
 * as their super class. So replace all their invocations with ones to equivalent
 * methods on [sandbox.java.lang.DJVMClass] that require [sandbox.java.lang.Enum]
 * instead.
 *
 * An annotation must implement [java.lang.annotation.Annotation] and have no other
 * interfaces. This means that the JVM cannot accept anything that implements
 * [sandbox.java.lang.annotation.Annotation] as an annotation! We must therefore
 * redirect the annotation-related methods on [Class] so that the DJVM can perform
 * some mappings.
 */
object RewriteClassMethods : Emitter {
    private fun prependClassArgTo(descriptor: String): String {
        return "(L$CLASS_NAME;${descriptor.substring(1)}"
    }

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == CLASS_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    if (isClassVirtualThunk(instruction.memberName)) {
                        invokeStatic(
                            owner = SANDBOX_CLASS_NAME,
                            name = instruction.memberName,
                            descriptor = prependClassArgTo(context.resolveDescriptor(instruction.descriptor))
                        )
                        preventDefault()
                     }

                INVOKESTATIC ->
                    if (isClassStaticThunk(instruction.memberName)) {
                        invokeStatic(
                            owner = SANDBOX_CLASS_NAME,
                            name = instruction.memberName,
                            descriptor = context.resolveDescriptor(instruction.descriptor)
                        )
                        preventDefault()
                    }
            }
        }
    }
}
