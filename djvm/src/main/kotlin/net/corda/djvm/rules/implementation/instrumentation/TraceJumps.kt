package net.corda.djvm.rules.implementation.instrumentation

import net.corda.djvm.code.Emitter
import net.corda.djvm.code.EmitterContext
import net.corda.djvm.code.Instruction
import net.corda.djvm.code.impl.EMIT_TRACING
import net.corda.djvm.code.instructions.BranchInstruction

/**
 * Emitter that will instrument the byte code such that all jumps get recorded.
 */
object TraceJumps : Emitter {

    override fun emit(context: EmitterContext, instruction: Instruction) = context.emit {
        if (instruction is BranchInstruction) {
            invokeInstrumenter("recordJump", "()V")
        }
    }

    override val priority: Int
        get() = EMIT_TRACING

}
