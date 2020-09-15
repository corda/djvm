package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.impl.CLASSLOADER_NAME
import net.corda.djvm.code.impl.SANDBOX_CLASSLOADER_NAME
import net.corda.djvm.code.impl.emit
import net.corda.djvm.code.impl.isClassLoaderStaticThunk
import net.corda.djvm.code.impl.isClassLoaderVirtualThunk
import net.corda.djvm.code.instructions.MemberAccessInstruction
import org.objectweb.asm.Opcodes.*

object RewriteClassLoaderMethods : Emitter {
    private fun prependClassLoaderArgTo(descriptor: String): String {
        return "(L$CLASSLOADER_NAME;${descriptor.substring(1)}"
    }

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is MemberAccessInstruction && instruction.className == CLASSLOADER_NAME) {
            when (instruction.operation) {
                INVOKEVIRTUAL ->
                    if (isClassLoaderVirtualThunk(instruction.memberName)) {
                        invokeStatic(
                            owner = SANDBOX_CLASSLOADER_NAME,
                            name = instruction.memberName,
                            descriptor = prependClassLoaderArgTo(context.resolveDescriptor(instruction.descriptor))
                        )
                        preventDefault()
                     }

                INVOKESTATIC ->
                    if (isClassLoaderStaticThunk(instruction.memberName)) {
                        invokeStatic(
                            owner = SANDBOX_CLASSLOADER_NAME,
                            name = instruction.memberName,
                            descriptor = context.resolveDescriptor(instruction.descriptor)
                        )
                        preventDefault()
                    }
            }
        }
    }
}
