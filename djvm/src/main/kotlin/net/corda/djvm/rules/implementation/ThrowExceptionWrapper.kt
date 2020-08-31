package net.corda.djvm.rules.implementation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.code.impl.FROM_DJVM
import org.objectweb.asm.Opcodes.ATHROW

/**
 * Converts a [sandbox.java.lang.Throwable] into a [java.lang.Throwable]
 * so that the JVM can throw it.
 */
object ThrowExceptionWrapper : Emitter {
    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        when (instruction.operation) {
            ATHROW -> {
                invokeStatic(DJVM_NAME, FROM_DJVM, "(Lsandbox/java/lang/Throwable;)Ljava/lang/Throwable;")
            }
        }
    }
}